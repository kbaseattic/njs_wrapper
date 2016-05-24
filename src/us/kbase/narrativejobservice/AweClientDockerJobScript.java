package us.kbase.narrativejobservice;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.ModuleVersionInfo;
import us.kbase.catalog.SelectModuleVersionParams;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple2;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.common.utils.ModuleMethod;
import us.kbase.common.utils.NetUtils;
import us.kbase.common.utils.UTCDateFormat;
import us.kbase.narrativejobservice.subjobs.CallbackServer;
import us.kbase.narrativejobservice.subjobs.CallbackServerConfigBuilder;
import us.kbase.narrativejobservice.subjobs.CallbackServerConfigBuilder.CallbackServerConfig;
import us.kbase.narrativejobservice.subjobs.ModuleRunVersion;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class AweClientDockerJobScript {
    
    // TODO consider an enum here
    public static final String DEV = RunAppBuilder.DEV;
    public static final String BETA = RunAppBuilder.BETA;
    public static final String RELEASE = RunAppBuilder.RELEASE;
    private static final long MAX_OUTPUT_SIZE = 15 * 1024;
    public static final Set<String> RELEASE_TAGS = RunAppBuilder.RELEASE_TAGS;
    
    public static final String CONFIG_FILE = "config.properties";

    public static void main(String[] args) throws Exception {
        System.out.println("Starting docker runner with args " +
            StringUtils.join(args, ", "));
        if (args.length != 2) {
            System.err.println("Usage: <program> <job_id> <job_service_url>");
            for (int i = 0; i < args.length; i++)
                System.err.println("\tArgument[" + i + "]: " + args[i]);
            System.exit(1);
        }
        String[] hostnameAndIP = getHostnameAndIP();
        final String jobId = args[0];
        String jobSrvUrl = args[1];
        String tokenStr = System.getenv("KB_AUTH_TOKEN");
        if (tokenStr == null || tokenStr.isEmpty())
            tokenStr = System.getProperty("KB_AUTH_TOKEN");  // For tests
        if (tokenStr == null || tokenStr.isEmpty())
            throw new IllegalStateException("Token is not defined");
        final AuthToken token = new AuthToken(tokenStr);
        final NarrativeJobServiceClient jobSrvClient = getJobClient(
                jobSrvUrl, token);
        UserAndJobStateClient ujsClient = null;
        Thread logFlusher = null;
        final List<LogLine> logLines = new ArrayList<LogLine>();
        DockerRunner.LineLogger log = null;
        Server callbackServer = null;
        try {
            Tuple2<RunJobParams, Map<String,String>> jobInput = jobSrvClient.getJobParams(jobId);
            Map<String, String> config = jobInput.getE2();
            final URL catalogURL = getURL(jobInput.getE2(),
                    NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
            final URI dockerURI = getURI(jobInput.getE2(),
                    NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI,
                    true);
            ujsClient = getUjsClient(jobInput.getE2(), token);
            RunJobParams job = jobInput.getE1();
            ujsClient.startJob(jobId, token.toString(), "running", "AWE job for " + job.getMethod(), 
                    new InitProgress().withPtype("none"), null);
            File jobDir = getJobDir(jobInput.getE2(), jobId);
            final ModuleMethod modMeth = new ModuleMethod(job.getMethod());
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
            File workDir = new File(jobDir, "workdir");
            if (!workDir.exists())
                workDir.mkdir();
            File scratchDir = new File(workDir, "tmp");
            if (!scratchDir.exists())
                scratchDir.mkdir();
            File inputFile = new File(workDir, "input.json");
            UObject.getMapper().writeValue(inputFile, rpc);
            File outputFile = new File(workDir, "output.json");
            File configFile = new File(workDir, CONFIG_FILE);
            PrintWriter pw = new PrintWriter(configFile);
            pw.println("[global]");
            pw.println("job_service_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL));
            pw.println("workspace_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL));
            pw.println("shock_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL));
            String kbaseEndpoint = config.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT);
            if (kbaseEndpoint != null)
                pw.println("kbase_endpoint = " + kbaseEndpoint);
            pw.close();
            ujsClient.updateJob(jobId, token.toString(), "running", null);
            log = new DockerRunner.LineLogger() {
                @Override
                public void logNextLine(String line, boolean isError) {
                    addLogLine(jobSrvClient, jobId, logLines, new LogLine().withLine(line).withIsError(isError ? 1L : 0L));
                }
            };
            log.logNextLine("Running on " + hostnameAndIP[0] + " (" + hostnameAndIP[1] + "), in " +
                    new File(".").getCanonicalPath(), false);
            String dockerRegistry = getDockerRegistryURL(config);
            CatalogClient catClient = new CatalogClient(catalogURL, token);
            catClient.setIsInsecureHttpConnectionAllowed(true);
            catClient.setAllSSLCertificatesTrusted(true);
            // the NJSW always passes the githash in service ver
            final String imageVersion = job.getServiceVer();
            final String requestedRelease = (String) job
                    .getAdditionalProperties().get(RunAppBuilder.REQ_REL);
            final ModuleInfo mi;
            final ModuleVersionInfo mvi;
            try {
                mi = catClient.getModuleInfo(new SelectOneModuleParams()
                        .withModuleName(modMeth.getModule()));
                mvi = catClient.getVersionInfo(new SelectModuleVersionParams()
                        .withModuleName(modMeth.getModule())
                        .withGitCommitHash(imageVersion));
            } catch (ServerException se) {
                throw new IllegalArgumentException(String.format(
                        "Error looking up module %s with githash %s: %s",
                        modMeth.getModule(), imageVersion,
                        se.getLocalizedMessage()));
            }
            String imageName = mvi.getDockerImgName();
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
                imageName = dockerRegistry + "/" +
                            modMeth.getModule().toLowerCase() + ":" +
                            imageVersion;
                //imageName = "kbase/" + moduleName.toLowerCase() + "." + imageVersion;
                log.logNextLine("Image is not stored in catalog, trying to guess: " + imageName, false);
            } else {
                log.logNextLine("Image name received from catalog: " + imageName, false);
            }
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
            // Starting up callback server
            final int callbackPort = NetUtils.findFreePort();
            final URL callbackUrl = CallbackServer.
                    getCallbackUrl(callbackPort);
            if (callbackUrl != null) {
                System.out.println("Job runner recieved callback URL: " +
                        callbackUrl);
                final ModuleRunVersion runver = new ModuleRunVersion(
                        new URL(mi.getGitUrl()), modMeth,
                        mvi.getGitCommitHash(), mvi.getVersion(),
                        requestedRelease);
                final CallbackServerConfig cbcfg = 
                        new CallbackServerConfigBuilder(config, callbackUrl,
                                jobDir.toPath(), log).build();
                final JsonServerServlet callback = new CallbackServer(
                        token, cbcfg, runver, job.getParams(),
                        job.getSourceWsObjects());
                callbackServer = new Server(callbackPort);
                final ServletContextHandler srvContext =
                        new ServletContextHandler(
                                ServletContextHandler.SESSIONS);
                srvContext.setContextPath("/");
                callbackServer.setHandler(srvContext);
                srvContext.addServlet(new ServletHolder(callback),"/*");
                callbackServer.start();
            } else {
                System.out.println("WARNING: No callback URL was recieved " +
                        "by the job runner. Local callbacks are disabled.");
            }
            // Calling Docker run
            new DockerRunner(dockerURI).run(
                    imageName, modMeth.getModule(),
                    inputFile, token, log, outputFile, false, 
                    refDataDir, null, callbackUrl);
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
            // flush logs to execution engine
            if (result.getError() != null) {
                String err = "";
                if (notNullOrEmpty(result.getError().getName())) {
                    err = result.getError().getName();
                }
                if (notNullOrEmpty(result.getError().getMessage())) {
                    if (!err.isEmpty()) {
                        err += ": ";
                    }
                    err += result.getError().getMessage();
                }
                if (notNullOrEmpty(result.getError().getError())) {
                    if (!err.isEmpty()) {
                        err += "\n";
                    }
                    err += result.getError().getError();
                }
                if (err == "")
                    err = "Unknown error (please ask administrator for details providing full output log)";
                log.logNextLine("Error: " + err, true);
            } else {
                log.logNextLine("Job is done", false);
            }
            flushLog(jobSrvClient, jobId, logLines);
            // push results to execution engine
            jobSrvClient.finishJob(jobId, result);
            ujsClient.completeJob(jobId, token.toString(), "done", null,
                    new Results());
            logFlusher.interrupt();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                flushLog(jobSrvClient, jobId, logLines);
            } catch (Exception ignore) {}
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.close();
            String stacktrace = sw.toString();
            try {
                if (log != null)
                    log.logNextLine("Fatal error: " + stacktrace, true);
                flushLog(jobSrvClient, jobId, logLines);
                logFlusher.interrupt();
            } catch (Exception ignore) {}
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
                ujsClient.completeJob(jobId, token.toString(), status,
                        stacktrace, null);
            }
        } finally {
            if (callbackServer != null)
                try {
                    callbackServer.stop();
                    System.out.println("Callback server was shutdown");
                } catch (Exception ignore) {
                    System.err.println("Error shutting down callback server: " + ignore.getMessage());
                }
        }
    }
    
    private static boolean notNullOrEmpty(final String s) {
        return s != null && !s.isEmpty();
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

    private static UserAndJobStateClient getUjsClient(
            final Map<String, String> config, 
            final AuthToken token)
            throws Exception {
        final URL ujsURL = getURL(config,
                NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        UserAndJobStateClient ret = new UserAndJobStateClient(ujsURL, token);
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
            AuthToken token) throws UnauthorizedException, IOException,
            MalformedURLException, TokenFormatException {
        final NarrativeJobServiceClient jobSrvClient =
                new NarrativeJobServiceClient(new URL(jobSrvUrl), token);
        jobSrvClient.setIsInsecureHttpConnectionAllowed(true);
        return jobSrvClient;
    }

    private static URL getURL(final Map<String, String> config,
            final String param) {
        final String urlStr = config.get(param);
        if (urlStr == null || urlStr.isEmpty()) {
            throw new IllegalStateException("Parameter '" + param +
                    "' is not defined in configuration");
        }
        try {
            return new URL(urlStr);
        } catch (MalformedURLException mal) {
            throw new IllegalStateException("The configuration parameter '" +
                    param + " = " + urlStr + "' is not a valid URL");
        }
    }
    
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
    
    public static String[] getHostnameAndIP() {
        String hostname = null;
        String ip = null;
        try {
            InetAddress ia = InetAddress.getLocalHost();
            ip = ia.getHostAddress();
            hostname = ia.getHostName();
        } catch (Throwable ignore) {}
        if (hostname == null) {
            try {
                hostname = System.getenv("HOSTNAME");
                if (hostname != null && hostname.isEmpty())
                    hostname = null;
            } catch (Throwable ignore) {}
        }
        if (ip == null && hostname != null) {
            try {
                ip = InetAddress.getByName(hostname).getHostAddress();
            } catch (Throwable ignore) {}
        }
        return new String[] {hostname == null ? "unknown" : hostname,
                ip == null ? "unknown" : ip};
    }
}
