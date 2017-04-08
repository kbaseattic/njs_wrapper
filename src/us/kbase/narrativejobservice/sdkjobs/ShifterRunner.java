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
import java.util.Map;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.kbase.auth.AuthToken;
import us.kbase.common.executionengine.LineLogger;
import us.kbase.common.utils.ProcessHelper;

import ch.qos.logback.classic.Level;

public class ShifterRunner {

    public static final int CANCELLATION_CHECK_PERIOD_SEC = 5;

    private final URI dockerURI;

    public ShifterRunner(final URI dockerURI) {
        this.dockerURI = null;
    }

    public File run(
            String imageName,
            final String moduleName,
            final File inputData,
            final AuthToken token,
            final LineLogger log,
            final File outputFile,
            final File refDataDir,
            final File optionalScratchDir,
            final URL callbackUrl,
            final String jobId,
            final List<Bind> additionalBinds,
            final CancellationChecker cancellationChecker,
            final Map<String, String> envVars)
            throws IOException, InterruptedException {

        if (!inputData.getName().equals("input.json"))
            throw new IllegalStateException("Input file has wrong name: " +
                    inputData.getName() + "(it should be named input.json)");
        File workDir = inputData.getCanonicalFile().getParentFile();
        File tokenFile = new File(workDir, "token");
        imageName = checkImagePulled(imageName, log);
        String cntName = null;
        try {
            FileWriter fw = new FileWriter(tokenFile);
            fw.write(token.getToken());
            fw.close();
            if (outputFile.exists())
                outputFile.delete();
            long suffix = System.currentTimeMillis();
            List<Bind> binds = new ArrayList<Bind>(Arrays.asList(new Bind(workDir.getAbsolutePath(),
                    new Volume("/kb/module/work"))));
            if (refDataDir != null)
                binds.add(new Bind(refDataDir.getAbsolutePath(), new Volume("/data"), AccessMode.ro));
            if (optionalScratchDir != null)
                binds.add(new Bind(optionalScratchDir.getAbsolutePath(), new Volume("/kb/module/work/tmp")));
            if (additionalBinds != null)
                binds.addAll(additionalBinds);
            List<String> envVarList = new ArrayList<String>();
            if (callbackUrl != null) {
                envVarList.add("SDK_CALLBACK_URL=" + callbackUrl);
            }
            if (envVars != null) {
                for (String envVarKey : envVars.keySet()) {
                    envVarList.add(envVarKey + "=" + envVars.get(envVarKey));
                }
            }
            // TODO: Should be able to use envVars directly
            Map<String, String> env = System.getenv();
            String [] environment = new String[env.size()+1];
            int j = 0;
            for (Map.Entry<String, String> entry : env.entrySet()) {
                environment[j] = entry.getKey() + "=" + entry.getValue();
                j++;
            }
            environment[j] = "SDK_CALLBACK_URL=" + callbackUrl;
            Process p = Runtime.getRuntime().exec(new String[] {"mydocker", "run", imageName, "async", "-v",Arrays.toString(binds.toArray())},
                                                  environment);
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
                                Thread.sleep(CANCELLATION_CHECK_PERIOD_SEC * 1000);
                                if (cancellationChecker.isJobCanceled()) {
                                    // Stop the container
                                    try {
                                        // TODO: need help here
                                        log.logNextLine("TODO: Kill SHifter container for module [" + moduleName + "]" +
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
            if (cancellationCheckingThread != null)
                cancellationCheckingThread.interrupt();
            //--------------------------------------------------
            //TODO: Cleanup
            if (outputFile.exists()) {
                return outputFile;
            } else {
                if (cancellationChecker != null && cancellationChecker.isJobCanceled())
                    return null;
                int exitCode;
                // TODO: Get exit code
                exitCode = 0; //resp2.getState().getExitCode();
                StringBuilder err = new StringBuilder();
                String msg = "Output file is not found, exit code is " + exitCode;
                if (err.length() > 0)
                    msg += ", errors: " + err;
                throw new IllegalStateException(msg);
            }
        } finally {
            // TOOD: Cleanup
            if (tokenFile.exists())
                try {
                    tokenFile.delete();
                } catch (Exception ignore) {}
        }
    }

    public String checkImagePulled(String imageName, LineLogger log)
            throws IOException {
        log.logNextLine("Image " + imageName + " is not pulled yet, pulling...", false);
        // TODO: Fix and catch error
        ProcessHelper.cmd("mydocker", "pull", imageName).exec(new File("."));
        return imageName;
    }
    // TODO: add findImageId function

    //private Container findContainerByNameOrIdPrefix(String nameOrIdPrefix) {

}
