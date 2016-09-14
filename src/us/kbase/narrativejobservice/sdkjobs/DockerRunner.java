package us.kbase.narrativejobservice.sdkjobs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.kbase.auth.AuthToken;
import us.kbase.common.executionengine.LineLogger;
import us.kbase.common.utils.ProcessHelper;

import ch.qos.logback.classic.Level;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class DockerRunner {
    private final URI dockerURI;
    
    public DockerRunner(final URI dockerURI) {
        this.dockerURI = dockerURI;
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
            final CancellationChecker cancellationChecker)
            throws IOException, InterruptedException {
        if (!inputData.getName().equals("input.json"))
            throw new IllegalStateException("Input file has wrong name: " + 
                    inputData.getName() + "(it should be named input.json)");
        File workDir = inputData.getCanonicalFile().getParentFile();
        File tokenFile = new File(workDir, "token");
        final DockerClient cl = createDockerClient();
        imageName = checkImagePulled(cl, imageName, log);
        String cntName = null;
        try {
            FileWriter fw = new FileWriter(tokenFile);
            fw.write(token.toString());
            fw.close();
            if (outputFile.exists())
                outputFile.delete();
            long suffix = System.currentTimeMillis();
            while (true) {
                cntName = moduleName.toLowerCase() + "_" + jobId.replace('-', '_') + "_" + suffix;
                if (findContainerByNameOrIdPrefix(cl, cntName) == null)
                    break;
                suffix++;
            }
            List<Bind> binds = new ArrayList<Bind>(Arrays.asList(new Bind(workDir.getAbsolutePath(), 
                    new Volume("/kb/module/work"))));
            if (refDataDir != null)
                binds.add(new Bind(refDataDir.getAbsolutePath(), new Volume("/data"), AccessMode.ro));
            if (optionalScratchDir != null)
                binds.add(new Bind(optionalScratchDir.getAbsolutePath(), new Volume("/kb/module/work/tmp")));
            if (additionalBinds != null)
                binds.addAll(additionalBinds);
            CreateContainerCmd cntCmd = cl.createContainerCmd(imageName)
                    .withName(cntName).withTty(true).withCmd("async").withBinds(
                            binds.toArray(new Bind[binds.size()]));
            if (callbackUrl != null)
                cntCmd = cntCmd.withEnv("SDK_CALLBACK_URL=" + callbackUrl);
            CreateContainerResponse resp = cntCmd.exec();
            final String cntId = resp.getId();
            Process p = Runtime.getRuntime().exec(new String[] {"docker", "start", "-a", cntId});
            List<Thread> workers = new ArrayList<Thread>();
            InputStream[] inputStreams = new InputStream[] {p.getInputStream(), p.getErrorStream()};
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
            Thread cancellationCheckingThread = null;
            if (cancellationChecker != null) {
                cancellationCheckingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!Thread.interrupted()) {
                            try {
                                Thread.sleep(1000);
                                if (cancellationChecker.isJobCancelled()) {
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
                                    break;
                                }
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }
                    }
                });
                cancellationCheckingThread.start();
            }
            for (Thread t : workers)
                t.join();
            p.waitFor();
            cl.waitContainerCmd(cntId).exec();
            if (cancellationCheckingThread != null)
                cancellationCheckingThread.interrupt();
            //--------------------------------------------------
            InspectContainerResponse resp2 = cl.inspectContainerCmd(cntId).exec();
            if (resp2.getState().isRunning()) {
                try {
                    Container cnt = findContainerByNameOrIdPrefix(cl, cntName);
                    cl.stopContainerCmd(cnt.getId()).exec();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                throw new IllegalStateException("Container was still running");
            }
            InputStream is = cl.logContainerCmd(cntId).withStdOut().withStdErr().exec();
            OutputStream os = new FileOutputStream(new File(workDir, "docker.log"));
            IOUtils.copy(is, os);
            os.close();
            is.close();
            if (outputFile.exists()) {
                return outputFile;
            } else {
                if (cancellationChecker != null && cancellationChecker.isJobCancelled())
                    return null;
                int exitCode = resp2.getState().getExitCode();
                StringBuilder err = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        cl.logContainerCmd(cntId).withStdErr(true).exec()));
                while (true) {
                    String l = br.readLine();
                    if (l == null)
                        break;
                    err.append(l).append("\n");
                }
                br.close();
                String msg = "Output file is not found, exit code is " + exitCode;
                if (err.length() > 0)
                    msg += ", errors: " + err;
                throw new IllegalStateException(msg);
            }
        } finally {
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
                } catch (Exception ignore) {}
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
        for (Image image: cl.listImagesCmd().withShowAll(all).exec()) {
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
            ch.qos.logback.classic.Logger log2 = (ch.qos.logback.classic.Logger)log;
            log2.setLevel(Level.ERROR);
        }
        if (dockerURI != null) {
            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                    .withUri(dockerURI.toASCIIString()).build();
            return DockerClientBuilder.getInstance(config).build();
        } else {
            return DockerClientBuilder.getInstance().build();
        }
    }
}
