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
import java.net.URL;
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

import us.kbase.auth.TokenFormatException;
import us.kbase.common.executionengine.JobRunnerConstants;
import us.kbase.common.executionengine.ModuleMethod;
import us.kbase.common.service.JacksonTupleModule;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.common.taskqueue2.JobStatuses;
import us.kbase.common.taskqueue2.RestartChecker;
import us.kbase.common.taskqueue2.TaskQueue;
import us.kbase.common.taskqueue2.TaskQueueConfig;
import us.kbase.narrativejobservice.db.ExecEngineMongoDb;
import us.kbase.narrativejobservice.db.MigrationToMongo;
import us.kbase.narrativejobservice.sdkjobs.ErrorLogger;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
//END_HEADER

/**
 * <p>Original spec-file module name: NarrativeJobService</p>
 * <pre>
 * </pre>
 */
public class NarrativeJobServiceServer extends JsonServerServlet {
    private static final long serialVersionUID = 1L;
    private static final String version = "0.0.1";
    private static final String gitUrl = "https://github.com/mrcreosote/njs_wrapper";
    private static final String gitCommitHash = "49902d721bb439d6a505faa4ee73aeb9f24a6eaa";

    //BEGIN_CLASS_HEADER
    public static final String SYS_PROP_KB_DEPLOYMENT_CONFIG = "KB_DEPLOYMENT_CONFIG";
    public static final String SERVICE_DEPLOYMENT_NAME = "NarrativeJobService";
    
    public static final String CFG_PROP_SCRATCH = "scratch";
    public static final String CFG_PROP_WORKSPACE_SRV_URL =
            JobRunnerConstants.CFG_PROP_WORKSPACE_SRV_URL;
    public static final String CFG_PROP_JOBSTATUS_SRV_URL =
            JobRunnerConstants.CFG_PROP_JOBSTATUS_SRV_URL;
    public static final String CFG_PROP_QUEUE_DB_DIR = "queue.db.dir";
    public static final String CFG_PROP_THREAD_COUNT = "thread.count";
    public static final String CFG_PROP_NJS_SRV_URL = "njs.srv.url";
    public static final String CFG_PROP_REBOOT_MODE = "reboot.mode";
    public static final String CFG_PROP_RUNNING_TASKS_PER_USER = "running.tasks.per.user";
    public static final String CFG_PROP_ADMIN_USER_NAME = "admin.user";
    public static final String CFG_PROP_SHOCK_URL =
            JobRunnerConstants.CFG_PROP_SHOCK_URL;
    public static final String CFG_PROP_AWE_SRV_URL = "awe.srv.url";
    public static final String CFG_PROP_MAX_JOB_SIZE = "max.job.size";
    public static final String CFG_PROP_AWE_CLIENT_SCRATCH = "awe.client.scratch";
    public static final String CFG_PROP_AWE_CLIENT_DOCKER_URI =
            JobRunnerConstants.CFG_PROP_AWE_CLIENT_DOCKER_URI;
    public static final String CFG_PROP_DOCKER_REGISTRY_URL = "docker.registry.url";
    public static final String AWE_CLIENT_SCRIPT_NAME = "run_async_srv_method.sh";
    public static final String CFG_PROP_CATALOG_SRV_URL = 
            JobRunnerConstants.CFG_PROP_CATALOG_SRV_URL;
    public static final String CFG_PROP_CATALOG_ADMIN_USER = "catalog.admin.user";
    public static final String CFG_PROP_CATALOG_ADMIN_PWD = "catalog.admin.pwd";
    public static final String CFG_PROP_KBASE_ENDPOINT =
            JobRunnerConstants.CFG_PROP_KBASE_ENDPOINT;
    public static final String CFG_PROP_SELF_EXTERNAL_URL = "self.external.url";
    public static final String CFG_PROP_REF_DATA_BASE = "ref.data.base";
    public static final String CFG_PROP_DEFAULT_AWE_CLIENT_GROUPS = "default.awe.client.groups";
    public static final String CFG_PROP_NARRATIVE_PROXY_SHARING_USER = "narrative.proxy.sharing.user";
    public static final String CFG_PROP_AWE_READONLY_ADMIN_USER = "awe.readonly.admin.user";
    public static final String CFG_PROP_AWE_READONLY_ADMIN_PWD = "awe.readonly.admin.pwd";
    public static final String CFG_PROP_MONGO_HOSTS = "mongodb-host";
    public static final String CFG_PROP_MONGO_DBNAME = "mongodb-database";
    public static final String CFG_PROP_MONGO_USER = "mongodb-user";
    public static final String CFG_PROP_MONGO_PWD = "mongodb-pwd";
    
    public static final String VERSION = "0.2.3";
    
    public static final String AWE_APPS_TABLE_NAME = "awe_apps";
    public static final String AWE_TASK_TABLE_NAME = "awe_tasks";
    public static final String AWE_LOGS_TABLE_NAME = "awe_logs";
    
    private static Throwable configError = null;
    private static String configPath = null;
    private static Map<String, String> config = null;
    
    private static TaskQueue taskHolder = null;
    private static TaskQueueConfig taskConfig = null;
    private static ExecEngineMongoDb db = null;
    
    private final ErrorLogger logger;
    
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
					+ "the configuration: is the ["+SERVICE_DEPLOYMENT_NAME+"] config group defined?");
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

    public static File getQueueDbDir() {
    	String ret = config().get(CFG_PROP_QUEUE_DB_DIR);
    	File dir = ret == null ? null : new File(ret);
    	return dir;
    }

    public static int getThreadCount() {
    	String ret = config().get(CFG_PROP_THREAD_COUNT);
    	if (ret == null)
    		throw new IllegalStateException("Parameter " + CFG_PROP_THREAD_COUNT + " is not defined in configuration");
    	return Integer.parseInt(ret);
    }

    public static String getNJSServiceURL() {
    	String ret = config().get(CFG_PROP_NJS_SRV_URL);
    	if (ret == null)
    		throw new IllegalStateException("Parameter " + CFG_PROP_NJS_SRV_URL + " is not defined in configuration");
    	return ret;
    }

    public static int getRunningTasksPerUser() {
    	String ret = config().get(CFG_PROP_RUNNING_TASKS_PER_USER);
    	if (ret == null)
    		throw new IllegalStateException("Parameter " + CFG_PROP_RUNNING_TASKS_PER_USER + " is not defined in configuration");
    	return Integer.parseInt(ret);
    }

    public static Set<String> getAdminUsers() {
    	String ret = config().get(CFG_PROP_ADMIN_USER_NAME);
    	if (ret == null)
    		throw new IllegalStateException("Parameter " + CFG_PROP_ADMIN_USER_NAME + " is not defined in configuration");
    	return new LinkedHashSet<String>(Arrays.asList(ret.split(Pattern.quote(","))));
    }

    public static boolean getRebootMode() {
    	try {
    		String ret = loadConfigFromDisk().get(CFG_PROP_REBOOT_MODE);
    		if (ret == null)
    			return false;
    		ret = ret.toLowerCase();
    		return ret.equals("true") || ret.equals("1");
    	} catch (Exception ex) {
    		return false;
    	}
    }
    
    public static synchronized TaskQueueConfig getTaskConfig() throws Exception {
    	if (taskConfig == null) {
    		int threadCount = getThreadCount();
    		File queueDbDir = getQueueDbDir();
    		final String wsUrl = getWorkspaceServiceURL();
    		final String ujsUrl = getUJSServiceURL();
    		final int runningTasksPerUser = getRunningTasksPerUser();
    		Map<String, String> allConfigProps = new LinkedHashMap<String, String>(config());
    		JobStatuses jobStatuses = new JobStatuses() {
				@Override
				public String createAndStartJob(String token, String status, String desc,
						String initProgressPtype, String estComplete) throws Exception {
    				return createJobClient(ujsUrl, token).createAndStartJob(token, status, desc, 
    						new InitProgress().withPtype(initProgressPtype), estComplete);
				}
				@Override
				public void updateJob(String job, String token, String status,
						String estComplete) throws Exception {
    				createJobClient(ujsUrl, token).updateJob(job, token, status, estComplete);
				}
				@Override
				public void completeJob(String job, String token, String status,
						String error, String wsUrl, String outRef) throws Exception {
					List<String> refs = new ArrayList<String>();
					if (outRef != null)
						refs.add(outRef);
    				createJobClient(ujsUrl, token).completeJob(job, token, status, error, 
    						new Results().withWorkspaceurl(wsUrl).withWorkspaceids(refs));
				}
			};
			taskConfig = new TaskQueueConfig(threadCount, queueDbDir, jobStatuses, wsUrl, 
					runningTasksPerUser, allConfigProps);
    	}
    	return taskConfig;
    }

	public static UserAndJobStateClient createJobClient(String jobSrvUrl, String token) throws IOException, JsonClientException {
		try {
			UserAndJobStateClient ret = new UserAndJobStateClient(new URL(jobSrvUrl), new AuthToken(token));
			ret.setIsInsecureHttpConnectionAllowed(true);
			ret.setAllSSLCertificatesTrusted(true);
			return ret;
		} catch (TokenFormatException e) {
			throw new JsonClientException(e.getMessage(), e);
		} catch (UnauthorizedException e) {
			throw new JsonClientException(e.getMessage(), e);
		}
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
	
    public static synchronized TaskQueue getTaskQueue() throws Exception {
    	if (taskHolder == null) {
    	    ExecEngineMongoDb db = getMongoDb(config());
            System.out.println("Initial queue size: " + db.getQueuedTasks().size());
    		TaskQueueConfig cfg = getTaskConfig();
			taskHolder = new TaskQueue(cfg, new RestartChecker() {
				@Override
				public boolean isInRestartMode() {
					return getRebootMode();
				}
			}, db, new RunAppBuilder());
    	}
    	return taskHolder;
    }
    
    private static NarrativeJobServiceClient getForwardClient(AuthToken authPart) throws Exception {
    	NarrativeJobServiceClient ret = new NarrativeJobServiceClient(new URL(getNJSServiceURL()), authPart);
    	ret.setAllSSLCertificatesTrusted(true);
    	ret.setIsInsecureHttpConnectionAllowed(true);
    	return ret;
    }
    
    public static String getKBaseEndpoint() throws Exception {
        String ret = config().get(CFG_PROP_KBASE_ENDPOINT);
        if (ret == null) {
            String wsUrl = getWorkspaceServiceURL();
            if (!wsUrl.endsWith("/ws"))
                throw new IllegalStateException("Parameter " + 
                        NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT + 
                        " is not defined in configuration");
            ret = wsUrl.replace("/ws", "");
        }
        return ret;
    }
    
    public static String getRefDataBase() throws Exception {
        return config().get(CFG_PROP_REF_DATA_BASE);
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
                        (String)rpcCallData.getContext().getAdditionalProperties().get("service_ver");
                    runJobParams.setServiceVer(serviceVer);
                    runJobParams.setMethod(modmeth.getModuleDotMethod());
                    runJobParams.setParams(paramsList);
                    runJobParams.setRpcContext(context);
                    result = new ArrayList<Object>(); 
                    result.add(runJob(runJobParams, new AuthToken(token),
                            rpcCallData.getContext()));
                } else if (modmeth.isCheck()) {
                    if (paramsList.size() == 1) {
                        String jobId = paramsList.get(0).asClassInstance(
                                String.class);
                        JobState jobState = checkJob(jobId,
                                new AuthToken(token),
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
    //END_CLASS_HEADER

    public NarrativeJobServiceServer() throws Exception {
        super("NarrativeJobService");
        //BEGIN_CONSTRUCTOR
       //TODO should check the config here and fail to start up if it's bad
        MigrationToMongo.migrate(getTaskQueue().getConfig(), getTaskQueue().getDb(), null);
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
        //END_CONSTRUCTOR
    }

    /**
     * <p>Original spec-file function name: run_app</p>
     * <pre>
     * </pre>
     * @param   app   instance of type {@link us.kbase.narrativejobservice.App App} (original type "app")
     * @return   instance of type {@link us.kbase.narrativejobservice.AppState AppState} (original type "app_state")
     */
    @JsonServerMethod(rpc = "NarrativeJobService.run_app", async=true)
    public AppState runApp(App app, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        AppState returnVal = null;
        //BEGIN run_app
        boolean forward = true;
        for (Step step : app.getSteps()) {
        	if (step.getParameters() == null || step.getInputValues() != null) {
        		forward = false;
        		break;
        	}
        }
        if (forward) {
        	returnVal = getForwardClient(authPart).runApp(app);
        } else {
            returnVal = RunAppBuilder.tryToRunAsOneStepAweScript(authPart.toString(), app, config());
            if (returnVal == null) {
                String appJobId = getTaskQueue().addTask(UObject.transformObjectToString(app), authPart.toString());
                returnVal = RunAppBuilder.initAppState(appJobId, config());
            }
        }
        //END run_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: check_app_state</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   instance of type {@link us.kbase.narrativejobservice.AppState AppState} (original type "app_state")
     */
    @JsonServerMethod(rpc = "NarrativeJobService.check_app_state", async=true)
    public AppState checkAppState(String jobId, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        AppState returnVal = null;
        //BEGIN check_app_state
        if (Util.isAweJobId(jobId)) {
        	returnVal = getForwardClient(authPart).checkAppState(jobId);
        } else {
        	returnVal = RunAppBuilder.loadAppState(jobId, config());
        	if (returnVal == null)
        		throw new IllegalStateException("Information is not available");
        	RunAppBuilder.checkIfAppStateNeedsUpdate(authPart.toString(), returnVal, config());
        }
        //END check_app_state
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: suspend_app</p>
     * <pre>
     * status - 'success' or 'failure' of action
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   parameter "status" of String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.suspend_app", async=true)
    public String suspendApp(String jobId, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        String returnVal = null;
        //BEGIN suspend_app
        if (Util.isAweJobId(jobId)) {
        	returnVal = getForwardClient(authPart).suspendApp(jobId);
        } else {
        	throw new IllegalStateException("This function is not supported for service calling APPs");
        }
        //END suspend_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: resume_app</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   parameter "status" of String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.resume_app", async=true)
    public String resumeApp(String jobId, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        String returnVal = null;
        //BEGIN resume_app
        if (Util.isAweJobId(jobId)) {
        	returnVal = getForwardClient(authPart).resumeApp(jobId);
        } else {
        	throw new IllegalStateException("This function is not supported for service calling APPs");
        }
        //END resume_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: delete_app</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   parameter "status" of String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.delete_app", async=true)
    public String deleteApp(String jobId, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        String returnVal = null;
        //BEGIN delete_app
        if (Util.isAweJobId(jobId)) {
        	returnVal = getForwardClient(authPart).deleteApp(jobId);
        } else {
        	AppState appState = RunAppBuilder.loadAppState(jobId, config());
        	if (appState != null) {
        		appState.setIsDeleted(1L);
        		returnVal = "App " + jobId + " was marked for deletion";
        	}
        }
        //END delete_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: list_config</p>
     * <pre>
     * </pre>
     * @return   instance of mapping from String to String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.list_config", authOptional=true, async=true)
    public Map<String,String> listConfig(AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        Map<String,String> returnVal = null;
        //BEGIN list_config
        returnVal = getForwardClient(authPart).listConfig();
        //END list_config
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: ver</p>
     * <pre>
     * Returns the current running version of the NarrativeJobService.
     * </pre>
     * @return   instance of String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.ver", async=true)
    public String ver(RpcContext jsonRpcContext) throws Exception {
        String returnVal = null;
        //BEGIN ver
        returnVal = VERSION;
        if (getTaskQueue().getStoppingMode())
        	returnVal += ", task-queue is in stopping mode";
        if (getRebootMode())
        	returnVal += ", service is in reboot mode";
        //END ver
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: status</p>
     * <pre>
     * Simply check the status of this service to see queue details
     * </pre>
     * @return   instance of type {@link us.kbase.narrativejobservice.Status Status}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.status", async=true)
    public Status status(RpcContext jsonRpcContext) throws Exception {
        Status returnVal = null;
        //BEGIN status
        int queued = getTaskQueue().getQueuedTasks();
        int running = getTaskQueue().getAllTasks() - queued;
        Map<String, String> safeConfig = new LinkedHashMap<String, String>();
        String[] keys = {CFG_PROP_AWE_SRV_URL, CFG_PROP_DOCKER_REGISTRY_URL, 
                CFG_PROP_JOBSTATUS_SRV_URL, CFG_PROP_NJS_SRV_URL, 
                CFG_PROP_QUEUE_DB_DIR, CFG_PROP_REBOOT_MODE, 
                CFG_PROP_RUNNING_TASKS_PER_USER, CFG_PROP_SCRATCH,
                CFG_PROP_SHOCK_URL, CFG_PROP_THREAD_COUNT,
                CFG_PROP_WORKSPACE_SRV_URL, CFG_PROP_KBASE_ENDPOINT,
                CFG_PROP_SELF_EXTERNAL_URL, CFG_PROP_REF_DATA_BASE,
                CFG_PROP_CATALOG_SRV_URL, CFG_PROP_AWE_CLIENT_DOCKER_URI};
        Map<String, String> config = config();
        for (String key : keys) {
            String value = config.get(key);
            if (value == null)
                value = "<not-defined>";
            safeConfig.put(key, value);
        }
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
        returnVal = new Status().withRebootMode(getRebootMode() ? 1L : 0L)
        		.withStoppingMode(getTaskQueue().getStoppingMode() ? 1L : 0L)
        		.withRunningTasksTotal((long)running)
        		.withRunningTasksPerUser(getTaskQueue().getRunningTasksPerUser())
        		.withTasksInQueue((long)queued)
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
     * <p>Original spec-file function name: list_running_apps</p>
     * <pre>
     * </pre>
     * @return   instance of list of type {@link us.kbase.narrativejobservice.AppState AppState} (original type "app_state")
     */
    @JsonServerMethod(rpc = "NarrativeJobService.list_running_apps", authOptional=true, async=true)
    public List<AppState> listRunningApps(AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        List<AppState> returnVal = null;
        //BEGIN list_running_apps
        if (!getAdminUsers().contains(authPart.getClientId()))
        	throw new IllegalStateException("Only admin of service can list internal apps");
        returnVal = RunAppBuilder.listRunningApps(config());
        //END list_running_apps
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: run_job</p>
     * <pre>
     * Start a new job (long running method of service registered in ServiceRegistery).
     * Such job runs Docker image for this service in script mode.
     * </pre>
     * @param   params   instance of type {@link us.kbase.narrativejobservice.RunJobParams RunJobParams}
     * @return   parameter "job_id" of original type "job_id" (A job id.)
     */
    @JsonServerMethod(rpc = "NarrativeJobService.run_job", async=true)
    public String runJob(RunJobParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        String returnVal = null;
        //BEGIN run_job
        returnVal = RunAppBuilder.runAweDockerScript(params, authPart.toString(), null, config(), null);
        //END run_job
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: get_job_params</p>
     * <pre>
     * Get job params necessary for job execution
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   multiple set: (1) parameter "params" of type {@link us.kbase.narrativejobservice.RunJobParams RunJobParams}, (2) parameter "config" of mapping from String to String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.get_job_params", tuple = true, async=true)
    public Tuple2<RunJobParams, Map<String,String>> getJobParams(String jobId, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        RunJobParams return1 = null;
        Map<String,String> return2 = null;
        //BEGIN get_job_params
        return2 = new LinkedHashMap<String, String>();
        return1 = RunAppBuilder.getAweDockerScriptInput(jobId, authPart.toString(), config(), return2);
        //END get_job_params
        Tuple2<RunJobParams, Map<String,String>> returnVal = new Tuple2<RunJobParams, Map<String,String>>();
        returnVal.setE1(return1);
        returnVal.setE2(return2);
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: add_job_logs</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @param   lines   instance of list of type {@link us.kbase.narrativejobservice.LogLine LogLine}
     * @return   parameter "line_number" of Long
     */
    @JsonServerMethod(rpc = "NarrativeJobService.add_job_logs", async=true)
    public Long addJobLogs(String jobId, List<LogLine> lines, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        Long returnVal = null;
        //BEGIN add_job_logs
        returnVal = (long)RunAppBuilder.addAweDockerScriptLogs(jobId, lines, authPart.toString(), config());
        //END add_job_logs
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: get_job_logs</p>
     * <pre>
     * </pre>
     * @param   params   instance of type {@link us.kbase.narrativejobservice.GetJobLogsParams GetJobLogsParams}
     * @return   instance of type {@link us.kbase.narrativejobservice.GetJobLogsResults GetJobLogsResults}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.get_job_logs", async=true)
    public GetJobLogsResults getJobLogs(GetJobLogsParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        GetJobLogsResults returnVal = null;
        //BEGIN get_job_logs
        returnVal = RunAppBuilder.getAweDockerScriptLogs(params.getJobId(), params.getSkipLines(), 
                authPart.toString(), getAdminUsers(), config());
        //END get_job_logs
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: finish_job</p>
     * <pre>
     * Register results of already started job
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @param   params   instance of type {@link us.kbase.narrativejobservice.FinishJobParams FinishJobParams}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.finish_job", async=true)
    public void finishJob(String jobId, FinishJobParams params, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        //BEGIN finish_job
        RunAppBuilder.finishAweDockerScript(jobId, params, authPart.toString(), logger, config());
        //END finish_job
    }

    /**
     * <p>Original spec-file function name: check_job</p>
     * <pre>
     * Check if a job is finished and get results/error
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   parameter "job_state" of type {@link us.kbase.narrativejobservice.JobState JobState}
     */
    @JsonServerMethod(rpc = "NarrativeJobService.check_job", async=true)
    public JobState checkJob(String jobId, AuthToken authPart, RpcContext jsonRpcContext) throws Exception {
        JobState returnVal = null;
        //BEGIN check_job
        returnVal = RunAppBuilder.checkJob(jobId, authPart.toString(), config());
        //END check_job
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
