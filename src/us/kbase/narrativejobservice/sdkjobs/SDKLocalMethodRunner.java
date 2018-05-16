package us.kbase.narrativejobservice.sdkjobs;

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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.google.common.html.HtmlEscapers;

import us.kbase.auth.AuthConfig;
import us.kbase.auth.AuthToken;
import us.kbase.auth.ConfigurableAuthService;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.GetSecureConfigParamsInput;
import us.kbase.catalog.ModuleVersion;
import us.kbase.catalog.SecureConfigParameter;
import us.kbase.catalog.SelectModuleVersion;
import us.kbase.catalog.VolumeMount;
import us.kbase.catalog.VolumeMountConfig;
import us.kbase.catalog.VolumeMountFilter;
import us.kbase.common.executionengine.CallbackServer;
import us.kbase.common.executionengine.CallbackServerConfigBuilder;
import us.kbase.common.executionengine.JobRunnerConstants;
import us.kbase.common.executionengine.LineLogger;
import us.kbase.common.executionengine.ModuleMethod;
import us.kbase.common.executionengine.ModuleRunVersion;
import us.kbase.common.executionengine.CallbackServerConfigBuilder.CallbackServerConfig;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple2;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.common.utils.NetUtils;
import us.kbase.narrativejobservice.CancelJobParams;
import us.kbase.narrativejobservice.CheckJobCanceledResult;
import us.kbase.narrativejobservice.FinishJobParams;
import us.kbase.narrativejobservice.JobState;
import us.kbase.narrativejobservice.JsonRpcError;
import us.kbase.narrativejobservice.LogLine;
import us.kbase.narrativejobservice.MethodCall;
import us.kbase.narrativejobservice.NarrativeJobServiceClient;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.narrativejobservice.RpcContext;
import us.kbase.narrativejobservice.RunJobParams;
import us.kbase.narrativejobservice.UpdateJobParams;
import us.kbase.narrativejobservice.subjobs.NJSCallbackServer;

public class SDKLocalMethodRunner {

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
        // We should skip token validation now because we don't have auth service URL yet.
        final AuthToken tempToken = new AuthToken(tokenStr, "<unknown>");
        final NarrativeJobServiceClient jobSrvClient = getJobClient(
                jobSrvUrl, tempToken);
        Thread logFlusher = null;
        final List<LogLine> logLines = new ArrayList<LogLine>();
        final LineLogger log = new LineLogger() {
            @Override
            public void logNextLine(String line, boolean isError) {
                addLogLine(jobSrvClient, jobId, logLines,
                        new LogLine().withLine(line)
                            .withIsError(isError ? 1L : 0L));
            }
        };
        Server callbackServer = null;
        try {
            JobState jobState = jobSrvClient.checkJob(jobId);
            if (jobState.getFinished() != null && jobState.getFinished() == 1L) {
                if (jobState.getCanceled() != null && jobState.getCanceled() == 1L) {
                    log.logNextLine("Job was canceled", false);
                } else {
                    log.logNextLine("Job was already done before", true);
                }
                flushLog(jobSrvClient, jobId, logLines);
                return;
            }
            Tuple2<RunJobParams, Map<String,String>> jobInput = jobSrvClient.getJobParams(jobId);
            Map<String, String> config = jobInput.getE2();
            ConfigurableAuthService auth = getAuth(config);
            // We couldn't validate token earlier because we didn't have auth service URL.
            AuthToken token = auth.validateToken(tokenStr);
            final URL catalogURL = getURL(config,
                    NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
            final URI dockerURI = getURI(config,
                    NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI,
                    true);
            RunJobParams job = jobInput.getE1();
            for (String msg : jobSrvClient.updateJob(new UpdateJobParams().withJobId(jobId)
                    .withIsStarted(1L)).getMessages()) {
                log.logNextLine(msg, false);
            }
            File jobDir = getJobDir(jobInput.getE2(), jobId);
            final ModuleMethod modMeth = new ModuleMethod(job.getMethod());
            RpcContext context = job.getRpcContext();
            if (context == null)
                context = new RpcContext().withRunId("");
            if (context.getCallStack() == null)
                context.setCallStack(new ArrayList<MethodCall>());
            context.getCallStack().add(new MethodCall().withJobId(jobId).withMethod(job.getMethod())
                    .withTime(DATE_FORMATTER.print(new DateTime())));
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
            File configFile = new File(workDir, JOB_CONFIG_FILE);
            String kbaseEndpoint = config.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT);
            String clientDetails = hostnameAndIP[1];
            String clientName = System.getenv("AWE_CLIENTNAME");
            if (clientName != null && !clientName.isEmpty()) {
                clientDetails += ", client-name=" + clientName;
            }
            log.logNextLine("Running on " + hostnameAndIP[0] + " (" + clientDetails + "), in " +
                    new File(".").getCanonicalPath(), false);
            String clientGroup = System.getenv("AWE_CLIENTGROUP");
            if (clientGroup == null)
                clientGroup = "<unknown>";
            log.logNextLine("Client group: " + clientGroup, false);
            String codeEeVer = NarrativeJobServiceServer.VERSION;
            String runtimeEeVersion = config.get(CFG_PROP_EE_SERVER_VERSION);
            if (runtimeEeVersion == null)
                runtimeEeVersion = "<unknown>";
            if (codeEeVer.equals(runtimeEeVersion)) {
                log.logNextLine("Server version of Execution Engine: " +
                        runtimeEeVersion + " (matches to version of runner script)", false);
            } else {
                log.logNextLine("WARNING: Server version of Execution Engine (" +
                        runtimeEeVersion + ") doesn't match to version of runner script " +
                        "(" + codeEeVer + ")", true);
            }
            CatalogClient catClient = new CatalogClient(catalogURL, token);
            catClient.setIsInsecureHttpConnectionAllowed(true);
            catClient.setAllSSLCertificatesTrusted(true);
            // the NJSW always passes the githash in service ver
            final String imageVersion = job.getServiceVer();
            final String requestedRelease = (String) job
                    .getAdditionalProperties().get(SDKMethodRunner.REQ_REL);
            final ModuleVersion mv;
            try {
                mv = catClient.getModuleVersion(new SelectModuleVersion()
                    .withModuleName(modMeth.getModule())
                    .withVersion(imageVersion));
            } catch (ServerException se) {
                throw new IllegalArgumentException(String.format(
                        "Error looking up module %s with version %s: %s",
                        modMeth.getModule(), imageVersion,
                        se.getLocalizedMessage()));
            }
            String imageName = mv.getDockerImgName();
            File refDataDir = null;
            String refDataBase = config.get(NarrativeJobServiceServer.CFG_PROP_REF_DATA_BASE);
            if (mv.getDataFolder() != null && mv.getDataVersion() != null) {
                if (refDataBase == null)
                    throw new IllegalStateException("Reference data parameters are defined for image but " + 
                            NarrativeJobServiceServer.CFG_PROP_REF_DATA_BASE + " property isn't set in configuration");
                refDataDir = new File(new File(refDataBase, mv.getDataFolder()), mv.getDataVersion());
                if (!refDataDir.exists())
                    throw new IllegalStateException("Reference data directory doesn't exist: " + refDataDir);
            }
            if (imageName == null) {
                throw new IllegalStateException("Image is not stored in catalog");
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
            // Let's check if there are some volume mount rules or secure configuration parameters
            // set up for this module
            List<Bind> additionalBinds = null;
            Map<String, String> envVars = null;
            List<SecureConfigParameter> secureCfgParams = null;
            String adminTokenStr = System.getenv("KB_ADMIN_AUTH_TOKEN");
            if (adminTokenStr == null || adminTokenStr.isEmpty())
                adminTokenStr = System.getProperty("KB_ADMIN_AUTH_TOKEN");  // For tests
            if (adminTokenStr != null && !adminTokenStr.isEmpty()) {
                final AuthToken adminToken = auth.validateToken(adminTokenStr);
                final CatalogClient adminCatClient = new CatalogClient(catalogURL, adminToken);
                adminCatClient.setIsInsecureHttpConnectionAllowed(true);
                adminCatClient.setAllSSLCertificatesTrusted(true);
                List<VolumeMountConfig> vmc = null;
                try {
                    vmc = adminCatClient.listVolumeMounts(new VolumeMountFilter().withModuleName(
                            modMeth.getModule()).withClientGroup(clientGroup)
                            .withFunctionName(modMeth.getMethod()));
                } catch (Exception ex) {
                    log.logNextLine("Error requesing volume mounts from Catalog: " + ex.getMessage(), true);
                }
                if (vmc != null && vmc.size() > 0) {
                    if (vmc.size() > 1)
                        throw new IllegalStateException("More than one rule for Docker volume mounts was found");
                    additionalBinds = new ArrayList<Bind>();
                    for (VolumeMount vm : vmc.get(0).getVolumeMounts()) {
                        boolean isReadOnly = vm.getReadOnly() != null && vm.getReadOnly() != 0L;
                        File hostDir = new File(processHostPathForVolumeMount(vm.getHostDir(), 
                                token.getUserName()));
                        if (!hostDir.exists()) {
                            if (isReadOnly) {
                                throw new IllegalStateException("Volume mount directory doesn't exist: " + 
                            hostDir);
                            } else {
                                hostDir.mkdirs();
                            }
                        }
                        String contDir = vm.getContainerDir();
                        AccessMode am = isReadOnly ?
                                AccessMode.ro : AccessMode.rw;
                        additionalBinds.add(new Bind(hostDir.getCanonicalPath(), new Volume(contDir), am));
                    }
                }
                secureCfgParams = adminCatClient.getSecureConfigParams(
                        new GetSecureConfigParamsInput().withModuleName(modMeth.getModule())
                        .withVersion(mv.getGitCommitHash()).withLoadAllVersions(0L));
                envVars = new TreeMap<String, String>();
                for (SecureConfigParameter param : secureCfgParams) {
                    envVars.put("KBASE_SECURE_CONFIG_PARAM_" + param.getParamName(), 
                            param.getParamValue());
                }
            }
            
            PrintWriter pw = new PrintWriter(configFile);
            pw.println("[global]");
            if (kbaseEndpoint != null)
                pw.println("kbase_endpoint = " + kbaseEndpoint);
            pw.println("job_service_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL));
            pw.println("workspace_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL));
            pw.println("shock_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL));
            pw.println("handle_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_HANDLE_SRV_URL));
            pw.println("srv_wiz_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_SRV_WIZ_URL));
            pw.println("njsw_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL));
            pw.println("auth_service_url = " + config.get(NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL));
            pw.println("auth_service_url_allow_insecure = " + 
                    config.get(NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM));
            if (secureCfgParams != null) {
                for (SecureConfigParameter param : secureCfgParams) {
                    pw.println(param.getParamName() + " = " + param.getParamValue());
                }
            }
            pw.close();
            
            // Cancellation checker
            CancellationChecker cancellationChecker = new CancellationChecker() {
                Boolean canceled = null;
                @Override
                public boolean isJobCanceled() {
                    if (canceled != null)
                        return canceled;
                    try {
                        final CheckJobCanceledResult jobState = jobSrvClient.checkJobCanceled(
                                new CancelJobParams().withJobId(jobId));
                        if (jobState.getFinished() != null && jobState.getFinished() == 1L) {
                            canceled = true;
                            if (jobState.getCanceled() != null && jobState.getCanceled() == 1L) {
                                // Print cancellation message after DockerRunner is done
                            } else {
                                log.logNextLine("Job was registered as finished by another worker",
                                        true);
                            }
                            flushLog(jobSrvClient, jobId, logLines);
                            return true;
                        }
                    } catch (Exception ex) {
                        log.logNextLine("Non-critical error checking for job cancelation - " +
                                String.format("Will check again in %s seconds. ",
                                        DockerRunner.CANCELLATION_CHECK_PERIOD_SEC) + 
                                "Error reported by execution engine was: " +
                                HtmlEscapers.htmlEscaper().escape(ex.getMessage()), true);
                    }
                    return false;
                }
            };
            // Starting up callback server
            String[] callbackNetworks = null;
            String callbackNetworksText = config.get(CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS);
            if (callbackNetworksText != null) {
                callbackNetworks = callbackNetworksText.trim().split("\\s*,\\s*");
            }
            final int callbackPort = NetUtils.findFreePort();
            final URL callbackUrl = CallbackServer.
                    getCallbackUrl(callbackPort, callbackNetworks);
            if (callbackUrl != null) {
                log.logNextLine("Job runner recieved callback URL: " +
                        callbackUrl, false);
                final ModuleRunVersion runver = new ModuleRunVersion(
                        new URL(mv.getGitUrl()), modMeth,
                        mv.getGitCommitHash(), mv.getVersion(),
                        requestedRelease);
                final CallbackServerConfig cbcfg = 
                        new CallbackServerConfigBuilder(config, callbackUrl,
                                jobDir.toPath(), Paths.get(refDataBase), log).build();
                final JsonServerServlet callback = new NJSCallbackServer(
                        token, cbcfg, runver, job.getParams(),
                        job.getSourceWsObjects(), additionalBinds, cancellationChecker);
                callbackServer = new Server(callbackPort);
                final ServletContextHandler srvContext =
                        new ServletContextHandler(
                                ServletContextHandler.SESSIONS);
                srvContext.setContextPath("/");
                callbackServer.setHandler(srvContext);
                srvContext.addServlet(new ServletHolder(callback),"/*");
                callbackServer.start();
            } else {
                if (callbackNetworks != null && callbackNetworks.length > 0) {
                    throw new IllegalStateException("No proper callback IP was found, " +
                            "please check 'awe.client.callback.networks' parameter in " +
                            "execution engine configuration");
                }
                log.logNextLine("WARNING: No callback URL was recieved " +
                        "by the job runner. Local callbacks are disabled.",
                        true);
            }
            // Calling Docker run
            new DockerRunner(dockerURI).run(imageName, modMeth.getModule(), inputFile, token, log,
                    outputFile, false, refDataDir, null, callbackUrl, jobId, additionalBinds,
                    cancellationChecker, envVars);
            if (cancellationChecker.isJobCanceled()) {
                log.logNextLine("Job was canceled", false);
                flushLog(jobSrvClient, jobId, logLines);
                logFlusher.interrupt();
                return;
            }
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
            String err = "Fatal error: " + sw.toString();
            if (ex instanceof ServerException) {
                err += "\nServer exception:\n" +
                        ((ServerException)ex).getData();
            }
            try {
                log.logNextLine(err, true);
                flushLog(jobSrvClient, jobId, logLines);
                logFlusher.interrupt();
            } catch (Exception ignore) {}
            try {
                FinishJobParams result = new FinishJobParams().withError(
                        new JsonRpcError().withCode(-1L).withName("JSONRPCError")
                        .withMessage("Job service side error: " + ex.getMessage())
                        .withError(err));
                jobSrvClient.finishJob(jobId, result);
            } catch (Exception ex2) {
                ex2.printStackTrace();
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
    
    public static String processHostPathForVolumeMount(String path, String username) {
        return path.replace("${username}", username);
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
    
    public static NarrativeJobServiceClient getJobClient(String jobSrvUrl,
            AuthToken token) throws UnauthorizedException, IOException,
            MalformedURLException {
        final NarrativeJobServiceClient jobSrvClient =
                new NarrativeJobServiceClient(new URL(jobSrvUrl), token);
        jobSrvClient.setIsInsecureHttpConnectionAllowed(true);
        return jobSrvClient;
    }
    
    private static ConfigurableAuthService getAuth(final Map<String, String> config) 
            throws Exception {
        String authUrl = config.get(NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL);
        if (authUrl == null) {
            throw new IllegalStateException("Deployment configuration parameter is not defined: " +
                    NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL);
        }
        String authAllowInsecure = config.get(
                NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM);
        final AuthConfig c = new AuthConfig().withKBaseAuthServerURL(new URL(authUrl));
        if ("true".equals(authAllowInsecure)) {
            c.withAllowInsecureURLs(true);
        }
        return new ConfigurableAuthService(c);
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
