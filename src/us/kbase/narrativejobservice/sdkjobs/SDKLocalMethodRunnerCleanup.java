package us.kbase.narrativejobservice.sdkjobs;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import us.kbase.auth.AuthToken;
import us.kbase.common.executionengine.JobRunnerConstants;
import us.kbase.common.executionengine.LineLogger;
import us.kbase.common.service.Tuple2;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.narrativejobservice.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SDKLocalMethodRunnerCleanup {

    private final static DateTimeFormatter DATE_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZoneUTC();

    public static final String DEV = JobRunnerConstants.DEV;
    public static final String BETA = JobRunnerConstants.BETA;
    public static final String RELEASE = JobRunnerConstants.RELEASE;
    public static final Set<String> RELEASE_TAGS = JobRunnerConstants.RELEASE_TAGS;
    private static final long MAX_OUTPUT_SIZE = JobRunnerConstants.MAX_IO_BYTE_SIZE;

    public static final String JOB_CONFIG_FILE = JobRunnerConstants.JOB_CONFIG_FILE;
    public static final String CFG_PROP_EE_SERVER_VERSION =
            JobRunnerConstants.CFG_PROP_EE_SERVER_VERSION;
    public static final String CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS =
            JobRunnerConstants.CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS;


    private static URI getURI(final Map<String, String> config,
                              final String param, boolean allowAbsent) {
        final String urlStr = config.get(param);
        if (urlStr == null || urlStr.isEmpty()) {
            if (allowAbsent) {
                return null;
            }
            throw new IllegalStateException("Parameter '" + param +
                    "' is not defined in configuration");
        }
        try {
            return new URI(urlStr);
        } catch (URISyntaxException use) {
            throw new IllegalStateException("The configuration parameter '" +
                    param + " = " + urlStr + "' is not a valid URI");
        }
    }


    /**
     * Submit a cancel job request to the NJS Client
     *
     * @param jobSrvClient
     * @param jobId
     * @throws Exception
     */
    public static void canceljob(NarrativeJobServiceClient jobSrvClient, String jobId) throws Exception {
        jobSrvClient.cancelJob(new CancelJobParams().withJobId(jobId));
    }


    public static void main(String[] args) throws Exception {
        System.out.println("Starting job cleaner  with args " +
                StringUtils.join(args, ", "));
        if (args.length != 2) {
            System.err.println("Usage: <program> <job_id> <job_service_url>");
            for (int i = 0; i < args.length; i++)
                System.err.println("\tArgument[" + i + "]: " + args[i]);
            System.exit(1);
        }


        final String jobId = args[0];
        String jobSrvUrl = args[1];
        String tokenStr = System.getenv("KB_AUTH_TOKEN");
        if (tokenStr == null || tokenStr.isEmpty())
            tokenStr = System.getProperty("KB_AUTH_TOKEN");  // For tests
        if (tokenStr == null || tokenStr.isEmpty())
            throw new IllegalStateException("Token is not defined");
        // We should skip token validation now because we don't have auth service URL yet.
        final AuthToken tempToken = new AuthToken(tokenStr, "<unknown>");
        final NarrativeJobServiceClient jobSrvClient = getJobClient(
                jobSrvUrl, tempToken);

        Tuple2<RunJobParams, Map<String, String>> jobInput = jobSrvClient.getJobParams(jobId);


        Map<String, String> config = jobInput.getE2();

        final URI dockerURI = getURI(config,
                NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI,
                true);


        File jobDir = getJobDir(config, jobId);
        File workDir = new File(jobDir, "workdir");
        File outputFile = new File(workDir, "output.json");


        final List<LogLine> logLines = new ArrayList<LogLine>();
        final LineLogger log = new LineLogger() {
            @Override
            public void logNextLine(String line, boolean isError) {
                addLogLine(jobSrvClient, jobId, logLines,
                        new LogLine().withLine(line)
                                .withIsError(isError ? 1L : 0L));
            }
        };


        JobState jobState = jobSrvClient.checkJob(jobId);
        //Job has ran successfully.
        if (jobState.getFinished() != null && jobState.getFinished() == 1L) {
            log.logNextLine("Cleaning up " + jobDir.toPath().toString(), false);

             new DockerRunner(dockerURI).runAlpineCleaner(jobDir);


            FileUtils.forceDelete(jobDir);
            return;
        }

        if (outputFile.length() > MAX_OUTPUT_SIZE) {
            Reader r = new FileReader(outputFile);
            char[] chars = new char[1000];
            r.read(chars);
            r.close();
            String error = "Method  returned value longer than " + MAX_OUTPUT_SIZE +
                    " bytes. This may happen as a result of returning actual data instead of saving it to " +
                    "kbase data stores (Workspace, Shock, ...) and returning reference to it. Returned " +
                    "value starts with \"" + new String(chars) + "...\"";

            log.logNextLine(error, true);
            log.logNextLine("Post Job Cleanup Detected Job Didn't Properly Finish", false);
            SDKLocalMethodRunner.finishJobPrematurely(error, jobId, log, dockerURI, jobSrvClient);
        } else if (!outputFile.exists() || (outputFile.exists() && outputFile.length() == 0)) {
            String error = "Couldn't find output file or output file size is empty";
            log.logNextLine(error, true);
            log.logNextLine("Post Job Cleanup Detected Job Didn't Properly Finish", false);
            SDKLocalMethodRunner.finishJobPrematurely(error, jobId, log, dockerURI, jobSrvClient);
        }
        flushLog(jobSrvClient, jobId, logLines);

    }

    private static synchronized void addLogLine(NarrativeJobServiceClient jobSrvClient,
                                                String jobId, List<LogLine> logLines, LogLine line) {
        logLines.add(line);
        if (line.getIsError() != null && line.getIsError() == 1L) {
            System.err.println(line.getLine());
        } else {
            System.out.println(line.getLine());
        }
    }

    private static synchronized void flushLog(NarrativeJobServiceClient jobSrvClient,
                                              String jobId, List<LogLine> logLines) {
        if (logLines.isEmpty())
            return;
        try {
            jobSrvClient.addJobLogs(jobId, logLines);
            logLines.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private static File getJobDir(Map<String, String> config, String jobId) {
        String rootDirPath = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_SCRATCH);
        File rootDir = new File(rootDirPath == null ? "." : rootDirPath);
        if (!rootDir.exists())
            rootDir.mkdirs();
        File ret = new File(rootDir, "job_" + jobId);
        if (!ret.exists())
            ret.mkdir();
        return ret;
    }


    public static NarrativeJobServiceClient getJobClient(String jobSrvUrl,
                                                         AuthToken token) throws UnauthorizedException, IOException,
            MalformedURLException {
        final NarrativeJobServiceClient jobSrvClient =
                new NarrativeJobServiceClient(new URL(jobSrvUrl), token);
        jobSrvClient.setIsInsecureHttpConnectionAllowed(true);
        return jobSrvClient;
    }

}
