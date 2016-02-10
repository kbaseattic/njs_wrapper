package us.kbase.narrativejobservice;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.ModuleVersionInfo;
import us.kbase.catalog.SelectModuleVersionParams;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.Tuple2;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.common.utils.UTCDateFormat;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class AweClientDockerJobScript {
    private static final long MAX_OUTPUT_SIZE = 15 * 1024;
    private static final Set<String> asyncVersionTags = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList("dev", "beta", "release")));

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: <program> <job_id> <job_service_url>");
            for (int i = 0; i < args.length; i++)
                System.err.println("\tArgument[" + i + "]: " + args[i]);
            System.exit(1);
        }
        final String jobId = args[0];
        String jobSrvUrl = args[1];
        String token = System.getenv("KB_AUTH_TOKEN");
        if (token == null || token.isEmpty())
            token = System.getProperty("KB_AUTH_TOKEN");  // For tests
        if (token == null || token.isEmpty())
            throw new IllegalStateException("Token is not defined");
        final NarrativeJobServiceClient jobSrvClient = getJobClient(jobSrvUrl, token);
        UserAndJobStateClient ujsClient = null;
        Thread logFlusher = null;
        final List<LogLine> logLines = new ArrayList<LogLine>();
        try {
            Tuple2<RunJobParams, Map<String,String>> jobInput = jobSrvClient.getJobParams(jobId);
            Map<String, String> config = jobInput.getE2();
            ujsClient = getUjsClient(config, token);
            RunJobParams job = jobInput.getE1();
            ujsClient.startJob(jobId, token, "running", "AWE job for " + job.getMethod(), 
                    new InitProgress().withPtype("none"), null);
            File jobDir = getJobDir(config, jobId);
            String moduleName = job.getMethod().split("\\.")[0];
            RpcContext context = job.getRpcContext();
            if (context == null)
                context = new RpcContext().withRunId("");
            if (context.getCallStack() == null)
                context.setCallStack(new ArrayList<MethodCall>());
            context.getCallStack().add(new MethodCall().withJobId(jobId).withMethod(job.getMethod())
                    .withTime(new UTCDateFormat().formatDate(new Date())));
            Map<String, Object> rpc = new LinkedHashMap<String, Object>();
            rpc.put("version", "1.1");
            rpc.put("method", job.getMethod());
            rpc.put("params", job.getParams());
            rpc.put("context", context);
            File inputFile = new File(jobDir, "input.json");
            UObject.getMapper().writeValue(inputFile, rpc);
            File outputFile = new File(jobDir, "output.json");
            File configFile = new File(jobDir, "config.properties");
            PrintWriter pw = new PrintWriter(configFile);
            pw.println("[global]");
            pw.println("job_service_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL));
            pw.println("workspace_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL));
            pw.println("shock_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL));
            String kbaseEndpoint = config.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT);
            if (kbaseEndpoint != null)
                pw.println("kbase_endpoint = " + kbaseEndpoint);
            pw.close();
            ujsClient.updateJob(jobId, token, "running", null);
            DockerRunner.LineLogger log = new DockerRunner.LineLogger() {
                @Override
                public void logNextLine(String line, boolean isError) throws Exception {
                    addLogLine(jobSrvClient, jobId, logLines, new LogLine().withLine(line).withIsError(isError ? 1L : 0L));
                }
            };
            String dockerRegistry = getDockerRegistryURL(config);
            CatalogClient catClient = getCatalogClient(config, token);
            String imageVersion = job.getServiceVer();
            String imageName = null;
            ModuleVersionInfo mvi = null;
            if (imageVersion == null || asyncVersionTags.contains(imageVersion)) {
                ModuleInfo mi = catClient.getModuleInfo(new SelectOneModuleParams().withModuleName(moduleName));
                if (imageVersion == null) {
                    mvi = mi.getRelease();
                } else if (imageVersion.equals("dev")) {
                    mvi = mi.getDev();
                } else if (imageVersion.equals("beta")) {
                    mvi = mi.getBeta();
                } else {
                    mvi = mi.getRelease();
                }
                if (mvi == null)
                    throw new IllegalStateException("Cannot extract " + imageVersion + " version for module: " + moduleName);
                imageVersion = mvi.getGitCommitHash();
            } else {
                try {
                    mvi = catClient.getVersionInfo(new SelectModuleVersionParams()
                            .withModuleName(moduleName).withGitCommitHash(imageVersion));
                } catch (Exception ex) {
                    throw new IllegalStateException("Error retrieving module version info about image " +
                            moduleName + " with version " + imageVersion, ex);
                }
            }
            imageName = mvi.getDockerImgName();
            File refDataDir = null;
            if (mvi.getDataFolder() != null && mvi.getDataVersion() != null) {
                String refDataBase = config.get(NarrativeJobServiceServer.CFG_PROP_REF_DATA_BASE);
                if (refDataBase == null)
                    throw new IllegalStateException("Reference data parameters are defined for image but " + 
                            NarrativeJobServiceServer.CFG_PROP_REF_DATA_BASE + " property isn't set in configuration");
                refDataDir = new File(new File(refDataBase, mvi.getDataFolder()), mvi.getDataVersion());
                if (!refDataDir.exists())
                    throw new IllegalStateException("Reference data directory doesn't exist: " + refDataDir);
            }
            if (imageName == null) {
                // TODO: We need to get rid of this line soon
                imageName = dockerRegistry + "/" +moduleName.toLowerCase() + ":" + imageVersion;
                //imageName = "kbase/" + moduleName.toLowerCase() + "." + imageVersion;
                log.logNextLine("Image is not stored in catalog, trying to guess: " + imageName, false);
            } else {
                log.logNextLine("Image recieved from catalog: " + imageName, false);
            }
            String dockerURI = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI);
            logFlusher = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            break;
                        }
                        flushLog(jobSrvClient, jobId, logLines);
                        if (Thread.currentThread().isInterrupted())
                            break;
                    }
                }
            });
            logFlusher.setDaemon(true);
            logFlusher.start();
            new DockerRunner(dockerURI).run(imageName, moduleName, inputFile, token, log, outputFile, false, refDataDir);
            if (outputFile.length() > MAX_OUTPUT_SIZE) {
                Reader r = new FileReader(outputFile);
                char[] chars = new char[1000];
                r.read(chars);
                r.close();
                String error = "Method " + job.getMethod() + " returned value longer than " + MAX_OUTPUT_SIZE + 
                        " bytes. This may happen as a result of returning actual data instead of saving it to " +
                        "kbase data stores (Workspace, Shock, ...) and returning reference to it. Returned " +
                        "value starts with \"" + new String(chars) + "...\"";
                throw new IllegalStateException(error);
            }
            FinishJobParams result = UObject.getMapper().readValue(outputFile, FinishJobParams.class);
            // save result into outputShockId;
            jobSrvClient.finishJob(jobId, result);
            ujsClient.completeJob(jobId, token, "done", null, new Results());
            flushLog(jobSrvClient, jobId, logLines);
            logFlusher.interrupt();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                flushLog(jobSrvClient, jobId, logLines);
                logFlusher.interrupt();
            } catch (Exception ignore) {}
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.close();
            String stacktrace = sw.toString();
            try {
                FinishJobParams result = new FinishJobParams().withError(
                        new JsonRpcError().withCode(-1L).withName("JSONRPCError")
                        .withMessage("Job service side error: " + ex.getMessage())
                        .withError(stacktrace));
                jobSrvClient.finishJob(jobId, result);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
            if (ujsClient != null) {
                String status = "Error: " + ex.getMessage();
                if (status.length() > 200)
                    status = status.substring(0, 197) + "...";
                ujsClient.completeJob(jobId, token, status, stacktrace, null);
            }
        }
    }

    private static synchronized void addLogLine(NarrativeJobServiceClient jobSrvClient,
            String jobId, List<LogLine> logLines, LogLine line) throws Exception {
        logLines.add(line);
        if (line.getIsError() != null && line.getIsError() == 1L) {
            System.err.println(line);
        } else {
            System.out.println(line);
        }
    }
    
    private static synchronized void flushLog(NarrativeJobServiceClient jobSrvClient,
            String jobId, List<LogLine> logLines) {
        try {
            jobSrvClient.addJobLogs(jobId, logLines);
            logLines.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String decamelize(final String s) {
        final Matcher m = Pattern.compile("([A-Z])").matcher(s.substring(1));
        return (s.substring(0, 1) + m.replaceAll("_$1")).toLowerCase();
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

    private static UserAndJobStateClient getUjsClient(Map<String, String> config, 
            String token) throws Exception {
        String ujsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        if (ujsUrl == null)
            throw new IllegalStateException("Parameter '" + 
                    NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL + "' is not defined in configuration");
        UserAndJobStateClient ret = new UserAndJobStateClient(new URL(ujsUrl), new AuthToken(token));
        ret.setIsInsecureHttpConnectionAllowed(true);
        return ret;
    }

    private static String getDockerRegistryURL(Map<String, String> config) {
        String drUrl = config.get(NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL);
        if (drUrl == null)
            throw new IllegalStateException("Parameter '" + NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL +
                    "' is not defined in configuration");
        return drUrl;
    }
    
    public static NarrativeJobServiceClient getJobClient(String jobSrvUrl,
            String token) throws UnauthorizedException, IOException,
            MalformedURLException, TokenFormatException {
        final NarrativeJobServiceClient jobSrvClient = new NarrativeJobServiceClient(new URL(jobSrvUrl), 
                new AuthToken(token));
        jobSrvClient.setIsInsecureHttpConnectionAllowed(true);
        return jobSrvClient;
    }

    private static CatalogClient getCatalogClient(Map<String, String> config, 
            String token) throws Exception {
        String catUrl = config.get(NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        if (catUrl == null)
            throw new IllegalStateException("Parameter '" + 
                    NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL + "' is not defined in configuration");
        CatalogClient ret = new CatalogClient(new URL(catUrl), new AuthToken(token));
        ret.setIsInsecureHttpConnectionAllowed(true);
        ret.setAllSSLCertificatesTrusted(true);
        return ret;
    }
}
