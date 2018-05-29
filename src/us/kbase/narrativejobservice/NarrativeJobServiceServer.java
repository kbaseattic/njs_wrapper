package us.kbase.narrativejobservice;

import java.io.File;
import java.util.List;
import java.util.Map;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.RpcContext;
import us.kbase.common.service.Tuple2;

//BEGIN_HEADER
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ini4j.Ini;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.common.executionengine.JobRunnerConstants;
import us.kbase.common.executionengine.ModuleMethod;
import us.kbase.common.service.JacksonTupleModule;
import us.kbase.common.service.UObject;
import us.kbase.narrativejobservice.db.ExecEngineMongoDb;
import us.kbase.narrativejobservice.sdkjobs.ErrorLogger;
import us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner;
//END_HEADER

/**
 * <p>Original spec-file module name: NarrativeJobService</p>
 * <pre>
 * </pre>
 */
public class NarrativeJobServiceServer extends JsonServerServlet {
    private static final long serialVersionUID = 1L;
    private static final String version = "0.0.1";
    private static final String gitUrl = "https://github.com/rsutormin/njs_wrapper";
    private static final String gitCommitHash = "18145b1030a53a711fb0615ed0edcadb7cf17dcf";

    //BEGIN_CLASS_HEADER
    public static final String SYS_PROP_KB_DEPLOYMENT_CONFIG = "KB_DEPLOYMENT_CONFIG";
    public static final String SERVICE_DEPLOYMENT_NAME = "NarrativeJobService";
    public static final String CFG_PROP_SCRATCH = "scratch";
    public static final String CFG_PROP_WORKSPACE_SRV_URL =
            JobRunnerConstants.CFG_PROP_WORKSPACE_SRV_URL;
    public static final String CFG_PROP_JOBSTATUS_SRV_URL =
            JobRunnerConstants.CFG_PROP_JOBSTATUS_SRV_URL;
    public static final String CFG_PROP_RUNNING_TASKS_PER_USER = "running.tasks.per.user";
    public static final String CFG_PROP_ADMIN_USER_NAME = "admin.user";
    public static final String CFG_PROP_SHOCK_URL =
            JobRunnerConstants.CFG_PROP_SHOCK_URL;
    public static final String CFG_PROP_HANDLE_SRV_URL =
            JobRunnerConstants.CFG_PROP_HANDLE_SRV_URL;
    public static final String CFG_PROP_SRV_WIZ_URL =
            JobRunnerConstants.CFG_PROP_SRV_WIZ_URL;
    public static final String CFG_PROP_CONDOR_MODE = "condor.mode";
    public static final String CFG_PROP_CONDOR_SUBMIT_DESC = "condor.submit.desc.file.path";
    public static final String CFG_PROP_AWE_SRV_URL = "awe.srv.url";
    public static final String CFG_PROP_AWE_CLIENT_SCRATCH = "awe.client.scratch";
    public static final String CFG_PROP_AWE_CLIENT_DOCKER_URI =
            JobRunnerConstants.CFG_PROP_AWE_CLIENT_DOCKER_URI;
    public static final String CFG_PROP_DOCKER_REGISTRY_URL = "docker.registry.url";
    public static final String AWE_CLIENT_SCRIPT_NAME = "run_async_srv_method.sh";
    public static final String CFG_PROP_CATALOG_SRV_URL =
            JobRunnerConstants.CFG_PROP_CATALOG_SRV_URL;
    public static final String CFG_PROP_CATALOG_ADMIN_USER = "catalog.admin.user";
    public static final String CFG_PROP_CATALOG_ADMIN_PWD = "catalog.admin.pwd";
    public static final String CFG_PROP_CATALOG_ADMIN_TOKEN = "catalog.admin.token";
    public static final String CFG_PROP_KBASE_ENDPOINT =
            JobRunnerConstants.CFG_PROP_KBASE_ENDPOINT;
    public static final String CFG_PROP_SELF_EXTERNAL_URL = JobRunnerConstants.CFG_PROP_NJSW_URL;
    public static final String CFG_PROP_REF_DATA_BASE = "ref.data.base";
    public static final String CFG_PROP_DEFAULT_AWE_CLIENT_GROUPS = "default.awe.client.groups";
    public static final String CFG_PROP_AWE_READONLY_ADMIN_USER = "awe.readonly.admin.user";
    public static final String CFG_PROP_AWE_READONLY_ADMIN_PWD = "awe.readonly.admin.pwd";
    public static final String CFG_PROP_AWE_READONLY_ADMIN_TOKEN = "awe.readonly.admin.token";

    public static final String CFG_PROP_MONGO_HOSTS = "mongodb-host";
    public static final String CFG_PROP_MONGO_DBNAME = "mongodb-database";
    public static final String CFG_PROP_MONGO_USER = "mongodb-user";
    public static final String CFG_PROP_MONGO_PWD = "mongodb-pwd";
    public static final String CFG_PROP_MONGO_HOSTS_UJS = "ujs-mongodb-host";
    public static final String CFG_PROP_MONGO_DBNAME_UJS = "ujs-mongodb-database";
    public static final String CFG_PROP_MONGO_USER_UJS = "ujs-mongodb-user";
    public static final String CFG_PROP_MONGO_PWD_UJS = "ujs-mongodb-pwd";

    public static final String CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS =
            JobRunnerConstants.CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS;
    public static final String CFG_PROP_AUTH_SERVICE_URL =
            JobRunnerConstants.CFG_PROP_AUTH_SERVICE_URL;
    public static final String CFG_PROP_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM =
            JobRunnerConstants.CFG_PROP_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM;

    public static final String VERSION = "0.2.11";

    private static Throwable configError = null;
    private static String configPath = null;
    private static Map<String, String> config = null;

    private static ExecEngineMongoDb db = null;

    private final ErrorLogger logger;

    private final static long maxRPCPackageSize = JobRunnerConstants.MAX_IO_BYTE_SIZE;

    public static Map<String, String> config() {
        if (config != null)
            return config;
        if (configError != null)
            throw new IllegalStateException("There was an error while loading configuration", configError);
        String configPath = System.getProperty(SYS_PROP_KB_DEPLOYMENT_CONFIG);
        if (configPath == null)
            configPath = System.getenv(SYS_PROP_KB_DEPLOYMENT_CONFIG);
        if (configPath == null) {
            configError = new IllegalStateException("Configuration file was not defined");
        } else {
            System.out.println(NarrativeJobServiceServer.class.getName() + ": Deployment config path was defined: " + configPath);
            try {
                NarrativeJobServiceServer.configPath = configPath;
                config = loadConfigFromDisk();
            } catch (Throwable ex) {
                System.out.println(NarrativeJobServiceServer.class.getName() + ": Error loading deployment config-file: " + ex.getMessage());
                configError = ex;
            }
        }
        if (config == null)
            throw new IllegalStateException("There was unknown error in service initialization when checking"
                    + "the configuration: is the [" + SERVICE_DEPLOYMENT_NAME + "] config group defined?");
        return config;
    }

    private static Map<String, String> loadConfigFromDisk() throws Exception {
        return new Ini(new File(configPath)).get(SERVICE_DEPLOYMENT_NAME);
    }

    public static File getTempDir() {
        String ret = config().get(CFG_PROP_SCRATCH);
        if (ret == null)
            throw new IllegalStateException("Parameter " + CFG_PROP_SCRATCH + " is not defined in configuration");
        File dir = new File(ret);
        if (!dir.exists())
            dir.mkdirs();
        return dir;
    }

    public static String getWorkspaceServiceURL() {
        String ret = config().get(CFG_PROP_WORKSPACE_SRV_URL);
        if (ret == null)
            throw new IllegalStateException("Parameter " + CFG_PROP_WORKSPACE_SRV_URL + " is not defined in configuration");
        return ret;
    }

    public static String getUJSServiceURL() {
        String ret = config().get(CFG_PROP_JOBSTATUS_SRV_URL);
        if (ret == null)
            throw new IllegalStateException("Parameter " + CFG_PROP_JOBSTATUS_SRV_URL + " is not defined in configuration");
        return ret;
    }

    public static Set<String> getAdminUsers() {
        String ret = config().get(CFG_PROP_ADMIN_USER_NAME);
        if (ret == null)
            throw new IllegalStateException("Parameter " + CFG_PROP_ADMIN_USER_NAME + " is not defined in configuration");
        return new LinkedHashSet<String>(Arrays.asList(ret.split(Pattern.quote(","))));
    }

    public static ExecEngineMongoDb getMongoDb(Map<String, String> config) throws Exception {
        if (db == null) {
            String hosts = config.get(CFG_PROP_MONGO_HOSTS);
            if (hosts == null)
                throw new IllegalStateException("Parameter " + CFG_PROP_MONGO_HOSTS + " is not defined in configuration");
            String dbname = config.get(CFG_PROP_MONGO_DBNAME);
            if (dbname == null)
                throw new IllegalStateException("Parameter " + CFG_PROP_MONGO_DBNAME + " is not defined in configuration");
            db = new ExecEngineMongoDb(hosts, dbname, config.get(CFG_PROP_MONGO_USER),
                    config.get(CFG_PROP_MONGO_PWD), null);
        }
        return db;
    }

    protected void processRpcCall(RpcCallData rpcCallData, String token, JsonServerSyslog.RpcInfo info,
                                  String requestHeaderXForwardedFor, ResponseStatusSetter response, OutputStream output,
                                  boolean commandLine) {
        if (rpcCallData.getMethod().startsWith("NarrativeJobService.")) {
            super.processRpcCall(rpcCallData, token, info, requestHeaderXForwardedFor, response, output, commandLine);
        } else {
            final ModuleMethod modmeth = new ModuleMethod(
                    rpcCallData.getMethod());
            List<UObject> paramsList = rpcCallData.getParams();
            List<Object> result = null;
            ObjectMapper mapper = new ObjectMapper().registerModule(new JacksonTupleModule());
            us.kbase.narrativejobservice.RpcContext context =
                    UObject.transformObjectToObject(rpcCallData.getContext(),
                            us.kbase.narrativejobservice.RpcContext.class);
            Exception exc = null;
            try {
                if (modmeth.isSubmit()) {
                    RunJobParams runJobParams = new RunJobParams();
                    String serviceVer = rpcCallData.getContext() == null ? null :
                            (String) rpcCallData.getContext().getAdditionalProperties().get("service_ver");
                    runJobParams.setServiceVer(serviceVer);
                    runJobParams.setMethod(modmeth.getModuleDotMethod());
                    runJobParams.setParams(paramsList);
                    runJobParams.setRpcContext(context);
                    result = new ArrayList<Object>();
                    result.add(runJob(runJobParams, validateToken(token),
                            rpcCallData.getContext()));
                } else if (modmeth.isCheck()) {
                    if (paramsList.size() == 1) {
                        String jobId = paramsList.get(0).asClassInstance(
                                String.class);
                        JobState jobState = checkJob(jobId,
                                validateToken(token),
                                rpcCallData.getContext());
                        Long finished = jobState.getFinished();
                        if (finished != 0L) {
                            Object error = jobState.getError();
                            if (error != null) {
                                Map<String, Object> ret =
                                        new LinkedHashMap<String, Object>();
                                ret.put("version", "1.1");
                                ret.put("error", error);
                                response.setStatus(HttpServletResponse
                                        .SC_INTERNAL_SERVER_ERROR);
                                mapper.writeValue(new UnclosableOutputStream(
                                        output), ret);
                                return;
                            }
                        }
                        result = new ArrayList<Object>();
                        result.add(jobState);
                    } else {
                        throw new IllegalArgumentException(
                                "Check method expects exactly one argument");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Method [" + rpcCallData.getMethod() +
                                    "] is not a valid method name for asynchronous job execution");
                }
                Map<String, Object> ret = new LinkedHashMap<String, Object>();
                ret.put("version", "1.1");
                ret.put("result", result);
                mapper.writeValue(new UnclosableOutputStream(output), ret);
                return;
            } catch (Exception ex) {
                exc = ex;
            }
            try {
                Map<String, Object> error = new LinkedHashMap<String, Object>();
                error.put("name", "JSONRPCError");
                error.put("code", -32601);
                error.put("message", exc.getLocalizedMessage());
                error.put("error", ExceptionUtils.getStackTrace(exc));
                Map<String, Object> ret = new LinkedHashMap<String, Object>();
                ret.put("version", "1.1");
                ret.put("error", error);
                mapper.writeValue(new UnclosableOutputStream(output), ret);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
                new Exception("Error sending error: " +
                        exc.getLocalizedMessage(), ex).printStackTrace();
            }
        }
    }

    private static class UnclosableOutputStream extends OutputStream {
        OutputStream inner;
        boolean isClosed = false;

        public UnclosableOutputStream(OutputStream inner) {
            this.inner = inner;
        }

        @Override
        public void write(int b) throws IOException {
            if (isClosed)
                return;
            inner.write(b);
        }

        @Override
        public void close() throws IOException {
            isClosed = true;
        }

        @Override
        public void flush() throws IOException {
            inner.flush();
        }

        @Override
        public void write(byte[] b) throws IOException {
            if (isClosed)
                return;
            inner.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (isClosed)
                return;
            inner.write(b, off, len);
        }
    }

    protected Long getMaxRPCPackageSize() {
        return maxRPCPackageSize;
    }
    //END_CLASS_HEADER

    public NarrativeJobServiceServer() throws Exception {
        super("NarrativeJobService");
        //BEGIN_CONSTRUCTOR
        logger = new ErrorLogger() {
            @Override
            public void logErr(Throwable err) {
                NarrativeJobServiceServer.this.logErr(err);
            }

            @Override
            public void logErr(String message) {
                NarrativeJobServiceServer.this.logErr(message);
            }
        };
        String authUrl = config().get(CFG_PROP_AUTH_SERVICE_URL);
        if (authUrl == null) {
            throw new IllegalStateException("Deployment configuration parameter is not defined: " +
                    CFG_PROP_AUTH_SERVICE_URL);
        }
        String aweAdminUser = config().get(CFG_PROP_AWE_READONLY_ADMIN_USER);
        if (aweAdminUser != null && aweAdminUser.trim().isEmpty()) {
            aweAdminUser = null;
        }
        String aweAdminToken = config().get(CFG_PROP_AWE_READONLY_ADMIN_TOKEN);
        if (aweAdminToken != null && aweAdminToken.trim().isEmpty()) {
            aweAdminToken = null;
        }
        if (aweAdminUser == null && aweAdminToken == null) {
            throw new IllegalStateException("Deployment configuration for AWE admin credentials " +
                    "is not defined: " + CFG_PROP_AWE_READONLY_ADMIN_USER + " or " +
                    CFG_PROP_AWE_READONLY_ADMIN_TOKEN);
        }
        String catAdminUser = config().get(CFG_PROP_CATALOG_ADMIN_USER);
        if (catAdminUser != null && catAdminUser.trim().isEmpty()) {
            catAdminUser = null;
        }
        String catAdminToken = config().get(CFG_PROP_CATALOG_ADMIN_TOKEN);
        if (catAdminToken != null && catAdminToken.trim().isEmpty()) {
            catAdminToken = null;
        }
        if (catAdminUser == null && catAdminToken == null) {
            throw new IllegalStateException("Deployment configuration for AWE admin credentials " +
                    "is not defined: " + CFG_PROP_CATALOG_ADMIN_USER + " or " +
                    CFG_PROP_CATALOG_ADMIN_TOKEN);
        }

        //END_CONSTRUCTOR
    }

    /**
     * <p>Original spec-file function name: list_config</p>
     * <pre>
     * </pre>
     *
     * @return instance of mapping from String to String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.list_config", authOptional = true, async = true)
    public Map<String, String> listConfig(AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        Map<String, String> returnVal = null;
        //BEGIN list_config
        Map<String, String> safeConfig = new LinkedHashMap<String, String>();
        String[] keys = {
                CFG_PROP_AWE_SRV_URL,
                CFG_PROP_DOCKER_REGISTRY_URL,
                CFG_PROP_JOBSTATUS_SRV_URL,
                CFG_PROP_SCRATCH,
                CFG_PROP_SHOCK_URL,
                CFG_PROP_WORKSPACE_SRV_URL,
                CFG_PROP_KBASE_ENDPOINT,
                CFG_PROP_SELF_EXTERNAL_URL,


                CFG_PROP_CONDOR_MODE,
                CFG_PROP_CONDOR_SUBMIT_DESC,


                CFG_PROP_REF_DATA_BASE,
                CFG_PROP_CATALOG_SRV_URL,
                CFG_PROP_AWE_CLIENT_DOCKER_URI,
                CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS
        };
        Map<String, String> config = config();
        for (String key : keys) {
            String value = config.get(key);
            if (value == null)
                value = "<not-defined>";
            safeConfig.put(key, value);
        }
        returnVal = safeConfig;
        //END list_config
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: ver</p>
     * <pre>
     * Returns the current running version of the NarrativeJobService.
     * </pre>
     *
     * @return instance of String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.ver", async = true)
    public String ver(RpcContext jsonRpcContext) throws Exception {
        String returnVal = null;
        //BEGIN ver
        returnVal = VERSION;
        //END ver
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: status</p>
     * <pre>
     * Simply check the status of this service to see queue details
     * </pre>
     *
     * @return instance of type {@link us.kbase.narrativejobservice.Status Status}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.status", async = true)
    public Status status(RpcContext jsonRpcContext) throws Exception {
        Status returnVal = null;
        //BEGIN status
        int queued = -1;
        int running = -1;
        Map<String, String> safeConfig = listConfig(null, jsonRpcContext);
        String gitCommit = null;
        try {
            Properties gitProps = new Properties();
            InputStream is = this.getClass().getResourceAsStream("git.properties");
            gitProps.load(is);
            is.close();
            gitCommit = gitProps.getProperty("commit");
        } catch (Exception ex) {
            gitCommit = "Error: " + ex.getMessage();
        }
        returnVal = new Status().withRebootMode(-1L)
                .withStoppingMode(-1L)
                .withRunningTasksTotal((long) running)
                .withRunningTasksPerUser(null)
                .withTasksInQueue((long) queued)
                .withConfig(safeConfig)
                .withGitCommit(gitCommit);

        // make warnings shut up
        @SuppressWarnings("unused")
        String foo = gitUrl;
        @SuppressWarnings("unused")
        String bar = gitCommitHash;
        @SuppressWarnings("unused")
        String baz = version;
        //END status
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: run_job</p>
     * <pre>
     * Start a new job (long running method of service registered in ServiceRegistery).
     * Such job runs Docker image for this service in script mode.
     * </pre>
     *
     * @param params instance of type {@link us.kbase.narrativejobservice.RunJobParams RunJobParams}
     * @return parameter "job_id" of original type "job_id" (A job id.)
     */
    @JsonServerMethod(rpc = "NarrativeJobService.run_job", async = true)
    public String runJob(RunJobParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        String returnVal = null;
        //BEGIN run_job
        System.gc();
        String aweClientGroups = SDKMethodRunner.requestClientGroups(config(), params.getMethod());
        returnVal = SDKMethodRunner.runJob(params, authPart, null, config(), aweClientGroups);
        //END run_job
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: get_job_params</p>
     * <pre>
     * Get job params necessary for job execution
     * </pre>
     *
     * @param jobId instance of original type "job_id" (A job id.)
     * @return multiple set: (1) parameter "params" of type {@link us.kbase.narrativejobservice.RunJobParams RunJobParams}, (2) parameter "config" of mapping from String to String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.get_job_params", tuple = true, async = true)
    public Tuple2<RunJobParams, Map<String, String>> getJobParams(String jobId, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        RunJobParams return1 = null;
        Map<String, String> return2 = null;
        //BEGIN get_job_params
        System.gc();
        return2 = new LinkedHashMap<String, String>();
        return1 = SDKMethodRunner.getJobInputParams(jobId, authPart, config(), return2);
        //END get_job_params
        Tuple2<RunJobParams, Map<String, String>> returnVal = new Tuple2<RunJobParams, Map<String, String>>();
        returnVal.setE1(return1);
        returnVal.setE2(return2);
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: update_job</p>
     * <pre>
     * </pre>
     *
     * @param params instance of type {@link us.kbase.narrativejobservice.UpdateJobParams UpdateJobParams}
     * @return instance of type {@link us.kbase.narrativejobservice.UpdateJobResults UpdateJobResults}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.update_job", async = true)
    public UpdateJobResults updateJob(UpdateJobParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        UpdateJobResults returnVal = null;
        //BEGIN update_job
        System.gc();
        returnVal = new UpdateJobResults().withMessages(SDKMethodRunner.updateJob(params, authPart, config()));
        //END update_job
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: add_job_logs</p>
     * <pre>
     * </pre>
     *
     * @param jobId instance of original type "job_id" (A job id.)
     * @param lines instance of list of type {@link us.kbase.narrativejobservice.LogLine LogLine}
     * @return parameter "line_number" of Long
     */
    @JsonServerMethod(rpc = "NarrativeJobService.add_job_logs", async = true)
    public Long addJobLogs(String jobId, List<LogLine> lines, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        Long returnVal = null;
        //BEGIN add_job_logs
        returnVal = (long) SDKMethodRunner.addJobLogs(jobId, lines, authPart, config());
        //END add_job_logs
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: get_job_logs</p>
     * <pre>
     * </pre>
     *
     * @param params instance of type {@link us.kbase.narrativejobservice.GetJobLogsParams GetJobLogsParams}
     * @return instance of type {@link us.kbase.narrativejobservice.GetJobLogsResults GetJobLogsResults}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.get_job_logs", async = true)
    public GetJobLogsResults getJobLogs(GetJobLogsParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        GetJobLogsResults returnVal = null;
        //BEGIN get_job_logs
        returnVal = SDKMethodRunner.getJobLogs(params.getJobId(), params.getSkipLines(),
                authPart, getAdminUsers(), config());
        //END get_job_logs
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: finish_job</p>
     * <pre>
     * Register results of already started job
     * </pre>
     *
     * @param jobId  instance of original type "job_id" (A job id.)
     * @param params instance of type {@link us.kbase.narrativejobservice.FinishJobParams FinishJobParams}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.finish_job", async = true)
    public void finishJob(String jobId, FinishJobParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        //BEGIN finish_job
        System.gc();
        SDKMethodRunner.finishJob(jobId, params, authPart, logger, config());
        //END finish_job
    }

    /**
     * <p>Original spec-file function name: check_job</p>
     * <pre>
     * Check if a job is finished and get results/error
     * </pre>
     *
     * @param jobId instance of original type "job_id" (A job id.)
     * @return parameter "job_state" of type {@link us.kbase.narrativejobservice.JobState JobState}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.check_job", async = true)
    public JobState checkJob(String jobId, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        JobState returnVal = null;
        //BEGIN check_job
        returnVal = SDKMethodRunner.checkJob(jobId, authPart, config());
        //END check_job
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: check_jobs</p>
     * <pre>
     * </pre>
     *
     * @param params instance of type {@link us.kbase.narrativejobservice.CheckJobsParams CheckJobsParams}
     * @return instance of type {@link us.kbase.narrativejobservice.CheckJobsResults CheckJobsResults}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.check_jobs", async = true)
    public CheckJobsResults checkJobs(CheckJobsParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        CheckJobsResults returnVal = null;
        //BEGIN check_jobs
        returnVal = SDKMethodRunner.checkJobs(params, authPart, config());
        //END check_jobs
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: cancel_job</p>
     * <pre>
     * </pre>
     *
     * @param params instance of type {@link us.kbase.narrativejobservice.CancelJobParams CancelJobParams}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.cancel_job", async = true)
    public void cancelJob(CancelJobParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        //BEGIN cancel_job
        SDKMethodRunner.cancelJob(params, authPart, config());
        //END cancel_job
    }

    /**
     * <p>Original spec-file function name: check_job_canceled</p>
     * <pre>
     * Check whether a job has been canceled. This method is lightweight compared to check_job.
     * </pre>
     *
     * @param params instance of type {@link us.kbase.narrativejobservice.CancelJobParams CancelJobParams}
     * @return parameter "result" of type {@link us.kbase.narrativejobservice.CheckJobCanceledResult CheckJobCanceledResult}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.check_job_canceled", async = true)
    public CheckJobCanceledResult checkJobCanceled(CancelJobParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        CheckJobCanceledResult returnVal = null;
        //BEGIN check_job_canceled
        returnVal = SDKMethodRunner.checkJobCanceled(params, authPart, config());
        //END check_job_canceled
        return returnVal;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            new NarrativeJobServiceServer().startupServer(Integer.parseInt(args[0]));
        } else if (args.length == 3) {
            JsonServerSyslog.setStaticUseSyslog(false);
            JsonServerSyslog.setStaticMlogFile(args[1] + ".log");
            new NarrativeJobServiceServer().processRpcCall(new File(args[0]), new File(args[1]), args[2]);
        } else {
            System.out.println("Usage: <program> <server_port>");
            System.out.println("   or: <program> <context_json_file> <output_json_file> <token>");
            return;
        }
    }
}
