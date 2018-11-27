package us.kbase.narrativejobservice.sdkjobs;

import ch.qos.logback.classic.Level;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.kbase.auth.AuthToken;
import us.kbase.common.executionengine.LineLogger;
import us.kbase.common.utils.ProcessHelper;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class DockerRunner {

    public static final int CANCELLATION_CHECK_PERIOD_SEC = 5;
    public static String dockerJobIdLogsDir = "docker_job_ids";
    public static DockerClient cl;

    private final URI dockerURI;
    private File tokenFile;
    private File workDir;

    public DockerRunner(final URI dockerURI) {
        this.dockerURI = dockerURI;
    }

    private List<Bind> getBinds(File workDir, File refDataDir, File optionalScratchDir, List<Bind> additionalBinds) {
        List<Bind> binds = new ArrayList<Bind>(Arrays.asList(new Bind(workDir.getAbsolutePath(),
                new Volume("/kb/module/work"))));
        if (refDataDir != null)
            binds.add(new Bind(refDataDir.getAbsolutePath(), new Volume("/data"), AccessMode.ro));
        if (optionalScratchDir != null)
            binds.add(new Bind(optionalScratchDir.getAbsolutePath(), new Volume("/kb/module/work/tmp")));
        if (additionalBinds != null)
            binds.addAll(additionalBinds);
        return binds;
    }

    private List<String> getEnvVars(URL callbackUrl, Map<String, String> envVars) {
        List<String> envVarList = new ArrayList<String>();
        if (callbackUrl != null) {
            envVarList.add("SDK_CALLBACK_URL=" + callbackUrl);
        }
        if (envVars != null) {
            for (String envVarKey : envVars.keySet()) {
                envVarList.add(envVarKey + "=" + envVars.get(envVarKey));
            }
        }
        return envVarList;
    }

    private void checkInputData(File inputData) throws IOException {
        workDir = inputData.getCanonicalFile().getParentFile();
        if (!inputData.getName().equals("input.json"))
            throw new IllegalStateException("Input file has wrong name: " +
                    inputData.getName() + "(it should be named input.json)");
    }

    private void setupJobToken(AuthToken token) throws IOException {
        tokenFile = new File(workDir, "token");
        FileWriter fw = new FileWriter(tokenFile);
        fw.write(token.getToken());
        fw.close();
    }

    private void setupForJob(File inputData, AuthToken token, File outputFile) throws IOException {
        checkInputData(inputData);
        setupJobToken(token);

        if (outputFile.exists())
            outputFile.delete();

    }

    private String generateContainerName(String moduleName, String jobId) {
        long suffix = System.currentTimeMillis();
        String cntName = null;
        while (true) {
            cntName = moduleName.toLowerCase() + "_" + jobId.replace('-', '_') + "_" + suffix;
            if (findContainerByNameOrIdPrefix(cl, cntName) == null)
                break;
            suffix++;
        }
        return cntName;
    }

    private CreateContainerCmd createContainerCommand(String imageName, String cntName,
                                                      List<Bind> binds, List<String> envVarList, Map<String, String> labels,
                                                      Map<String, String> resourceRequirements,
                                                      String parentCgroup,
                                                      LineLogger log) {


        CreateContainerCmd cntCmd = cl.createContainerCmd(imageName)
                .withName(cntName).withTty(true).withCmd("async").withBinds(
                        binds.toArray(new Bind[binds.size()]));
        cntCmd.withEnv(envVarList.toArray(new String[envVarList.size()]));

        String miniKB = System.getenv("MINI_KB");
        if (miniKB != null && !miniKB.isEmpty() && miniKB.equals("true")) {
            cntCmd.withNetworkMode("mini_kb_default");
        }
        cntCmd.withLabels(labels);

        HostConfig cpuMemoryLimiter = setJobRequirements(resourceRequirements, log);
        if (cpuMemoryLimiter != null) {
            //You have to reset binds and reset network unfortunately...
            cpuMemoryLimiter.withBinds(new Binds(binds.toArray(new Bind[binds.size()])));
            if (miniKB != null && !miniKB.isEmpty() && miniKB.equals("true")) {
                cpuMemoryLimiter.withNetworkMode("mini_kb_default");
            }
            cntCmd.withHostConfig(cpuMemoryLimiter);
        }


        if (parentCgroup != null) {
            System.out.println("About to set cgroup" + parentCgroup);
            cntCmd.withCgroupParent(parentCgroup);
        }

        return cntCmd;

    }


    private void logContainerNameId(String cntId, String cntName) throws IOException {
        //Create a log of all docker jobs
        new File(dockerJobIdLogsDir).mkdirs();
        File logFile = new File(dockerJobIdLogsDir + "/" + cntName);
        FileUtils.writeStringToFile(logFile, cntId);
    }


    private Thread createCancellationChecking(CancellationChecker cancellationChecker, String cntId, String moduleName, LineLogger log) {
        Thread cancellationCheckingThread = null;
        if (cancellationChecker != null) {
            cancellationCheckingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.interrupted()) {
                        try {
                            Thread.sleep(CANCELLATION_CHECK_PERIOD_SEC * 1000);
                            if (cancellationChecker.isJobCanceled()) {
                                // Stop the container
                                try {
                                    cl.stopContainerCmd(cntId).exec();

                                    log.logNextLine("Docker container for module [" + moduleName + "]" +
                                            " was successfully stopped during job cancellation", false);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    log.logNextLine("Error stopping docker container for module [" +
                                                    moduleName + "] during job cancellation: " + ex.getMessage(),
                                            true);
                                }
                                try {
                                    log.logNextLine("Attemping to kill subjobs.", false);
                                    killSubJobs();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                break;
                            }
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                }
            });
        }
        return cancellationCheckingThread;
    }

    private void cleanupAfterJob(String cntName, boolean removeImage, String imageName) {
        if (cntName != null) {
            Container cnt = findContainerByNameOrIdPrefix(cl, cntName);
            if (cnt != null) {
                try {
                    cl.removeContainerCmd(cnt.getId()).withRemoveVolumes(true).exec();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (removeImage) {
            try {
                Image img = findImageId(cl, imageName);
                cl.removeImageCmd(img.getId()).exec();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (tokenFile.exists())
            try {
                tokenFile.delete();
            } catch (Exception ignore) {
            }
    }


    public File run(
            String imageName,
            final String moduleName,
            final File inputData,
            final AuthToken token,
            final LineLogger log,
            final File outputFile,
            final boolean removeImage,
            final File refDataDir,
            final File optionalScratchDir,
            final URL callbackUrl,
            final String jobId,
            final List<Bind> additionalBinds,
            final CancellationChecker cancellationChecker,
            final Map<String, String> envVars,
            final Map<String, String> labels,
            final Map<String, String> resourceRequirements,
            final String parentCgroup,
            final String timeout)
            throws IOException, InterruptedException {

        String cntName = null;
        String cntId = null;

        try {
            cl = createDockerClient();
            setupForJob(inputData, token, outputFile);
            imageName = checkImagePulled(cl, imageName, log);
            cntName = generateContainerName(moduleName, jobId);
            List<Bind> binds = getBinds(workDir, refDataDir, optionalScratchDir, additionalBinds);
            List<String> envVarList = getEnvVars(callbackUrl, envVars);
            CreateContainerCmd cntCmd = createContainerCommand(imageName, cntName, binds, envVarList, labels, resourceRequirements, parentCgroup, log);
            cntId = cntCmd.exec().getId();
            logContainerNameId(cntId, cntName);


            Process p = Runtime.getRuntime().exec(new String[]{"docker", "start", "-a", cntId});
            List<Thread> workers = new ArrayList<Thread>();
            InputStream[] inputStreams = new InputStream[]{p.getInputStream(), p.getErrorStream()};
            for (int i = 0; i < inputStreams.length; i++) {
                final InputStream is = inputStreams[i];
                final boolean isError = i == 1;
                Thread ret = new Thread(new Runnable() {
                    public void run() {
                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                            while (true) {
                                String line = br.readLine();
                                if (line == null)
                                    break;
                                log.logNextLine(line, isError);
                            }
                            br.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new IllegalStateException("Error reading data from executed container", e);
                        }
                    }
                });
                ret.start();
                workers.add(ret);
            }

            Thread cancellationCheckingThread = createCancellationChecking(cancellationChecker, cntId, moduleName, log);
            if (cancellationCheckingThread != null)
                cancellationCheckingThread.start();


            for (Thread t : workers)
                t.join();
            log.logNextLine("Job will automatically timeout in " + timeout + " seconds" , false);
            p.waitFor(Long.parseLong(timeout), TimeUnit.SECONDS);

            int containerExitCode = cl.waitContainerCmd(cntId).exec(new WaitContainerResultCallback()).awaitStatusCode();
            if (containerExitCode == 125 || containerExitCode == 126 || containerExitCode == 128) {
                log.logNextLine("STATUS CODE=" + containerExitCode, true);
                InspectContainerResponse icr = cl.inspectContainerCmd(cntId).exec();
                log.logNextLine(icr.toString(), true);
            }

            if (cancellationCheckingThread != null)
                cancellationCheckingThread.interrupt();
            //--------------------------------------------------
            InspectContainerResponse resp2 = cl.inspectContainerCmd(cntId).exec();
            if (resp2.getState() != null && resp2.getState().getRunning()) {
                try {
                    Container cnt = findContainerByNameOrIdPrefix(cl, cntName);
                    cl.stopContainerCmd(cnt.getId()).exec();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                log.logNextLine("Container was still running after timeout!", true);
            }


            if (outputFile.exists()) {
                return outputFile;
            }
            if (cancellationChecker != null && cancellationChecker.isJobCanceled())
                return null;

            int exitCode = resp2.getState().getExitCode();
            String msg = "Output file is not found, exit code is " + exitCode;
            throw new IllegalStateException(msg);


        } finally {
            cleanupAfterJob(cntName, removeImage, imageName);
        }
    }

    //TODO UNUSED FOR NOW
    private void getLogs(String cntId, String workDir) throws Exception {
        LogContainerCmd logContainerCmd = cl.logContainerCmd(cntId).withStdOut(true).withStdErr(true).withTimestamps(true);

        final List<String> logs = new ArrayList<>();
        final List<String> err_logs = new ArrayList<>();

        try {
            logContainerCmd.exec(new LogContainerResultCallback() {
                @Override
                public void onNext(Frame item) {
                    logs.add(item.toString());
                    if (item.getStreamType().equals(StreamType.STDERR)) {
                        err_logs.add(item.toString());
                    }
                }
            }).awaitCompletion(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        FileWriter writer = new FileWriter(new File(workDir, "docker.log"));
        for (String str : logs) {
            writer.write(str);
        }
        writer.close();
    }


    /**
     * Get a list of subjob docker ids from the dockerJobIdLogsDir
     *
     * @return A list of subjob ids
     * @throws Exception
     */
    public static List<String> getSubJobDockerIds() throws Exception {
        File folder = new File(dockerJobIdLogsDir);
        List<String> files = new ArrayList<>();
        if (folder.exists()) {
            File[] listFiles = folder.listFiles();
            if (listFiles != null) {
                for (final File fileEntry : listFiles) {
                    if (!fileEntry.isDirectory()) {
                        String text = FileUtils.readFileToString(fileEntry);
                        files.add(text.replace(System.getProperty("line.separator"), ""));
                    }
                }
            }
        }
        return files;
    }

    /**
     * Get a list of subjob ids, and then send a cl.killContainerCmd to them all.
     *
     * @throws Exception
     */
    public void killSubJobs() throws Exception {
        List<String> subJobIds = getSubJobDockerIds();

        if (subJobIds != null) {
            for (final String subjobid : subJobIds) {
                System.out.println("Attempting to kill job due to cancellation or sig-kill:" + subjobid);
                try {
                    cl.killContainerCmd(subjobid).exec();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("No subjobs to kill");
        }
    }

    public String checkImagePulled(DockerClient cl, String imageName, LineLogger log)
            throws IOException {
        if (findImageId(cl, imageName) == null) {
            log.logNextLine("Image " + imageName + " is not pulled yet, pulling...", false);
            ProcessHelper.cmd("docker", "pull", imageName).exec(new File("."));
            if (findImageId(cl, imageName) == null) {
                throw new IllegalStateException("Image was not found: " + imageName);
            } else {
                log.logNextLine("Image " + imageName + " is pulled successfully", false);
            }
        }
        return imageName;
    }

    private Image findImageId(DockerClient cl, String imageTagOrIdPrefix) {
        return findImageId(cl, imageTagOrIdPrefix, false);
    }

    private Image findImageId(DockerClient cl, String imageTagOrIdPrefix, boolean all) {
        List<Image> imageList = cl.listImagesCmd().withShowAll(all).exec();

        for (Image image : imageList) {
            if (image.getId().startsWith(imageTagOrIdPrefix))
                return image;
            if (image.getRepoTags() == null)
                continue;
            for (String tag : image.getRepoTags())
                if (tag.equals(imageTagOrIdPrefix))
                    return image;
        }
        return null;
    }

    private Container findContainerByNameOrIdPrefix(DockerClient cl, String nameOrIdPrefix) {
        for (Container cnt : cl.listContainersCmd().withShowAll(true).exec()) {
            if (cnt.getId().startsWith(nameOrIdPrefix))
                return cnt;
            if (cnt.getNames() == null)
                continue;
            for (String name : cnt.getNames())
                if (name.equals(nameOrIdPrefix) || name.equals("/" + nameOrIdPrefix))
                    return cnt;
        }
        return null;
    }

    public DockerClient createDockerClient() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "ERROR");
        Logger log = LoggerFactory.getLogger("com.github.dockerjava");
        if (log instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log2 = (ch.qos.logback.classic.Logger) log;
            log2.setLevel(Level.ERROR);
        }
        if (dockerURI != null) {
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(dockerURI.toASCIIString()).build();

            return DockerClientBuilder.getInstance(config).build();
        } else {
            return DockerClientBuilder.getInstance().build();
        }
    }

    /**
     * Impose cpu and memory limits into a hostconfig
     *
     * @param resourceRequirements request_cpus and request_memory to be translated into a cpu limit and a soft memory limit
     * @param log                  logger
     * @return a hostconfig with constrainted memory and or cpu
     */
    public HostConfig setJobRequirements(Map<String, String> resourceRequirements, LineLogger log) {
        if (resourceRequirements == null || resourceRequirements.isEmpty()) {
            return null;
        }
        log.logNextLine("Setting requirements", true);
        HostConfig cpuMemoryLimiter = new HostConfig();
        for (String resourceKey : resourceRequirements.keySet()) {
            if (resourceKey.equals("request_cpus")) {
                int inputCores = Integer.parseInt(resourceRequirements.get("request_cpus"));
                int DEFAULT_CPU_PERIOD = 100000;
                int CPU_QUOTA_CONST = 100000;
                int cpuQuota = (int) (inputCores * CPU_QUOTA_CONST);
                cpuMemoryLimiter.withCpuQuota(cpuQuota);
                cpuMemoryLimiter.withCpuPeriod(DEFAULT_CPU_PERIOD);
                log.logNextLine("Setting request_cpus Requirements to " + inputCores, false);
                log.logNextLine("Setting cpuQuota  to " + cpuQuota, false);
                log.logNextLine("Setting withCpuPeriod  to " + DEFAULT_CPU_PERIOD, false);
            }
            if (resourceKey.equals("request_memory")) {
                String inputMemory = resourceRequirements.get("request_memory").replace("MB", "");
                long inputMemoryBytes = Long.parseLong(inputMemory) * 1000000L;
                //cpuMemoryLimiter.withMemory(inputMemoryBytes); //hard memory limit
                cpuMemoryLimiter.withMemoryReservation(inputMemoryBytes); //soft memory limit
                log.logNextLine("Setting request_memory Requirements to " + inputMemory, false);
                log.logNextLine("Setting withMemoryReservation  to " + inputMemoryBytes, false);
                if (Long.parseLong(inputMemory) <= 500) {
                    log.logNextLine("WARNING: withMemoryReservation MIGHT BE TOO LOW ", false);
                }
            }
        }
        return cpuMemoryLimiter;
    }

}