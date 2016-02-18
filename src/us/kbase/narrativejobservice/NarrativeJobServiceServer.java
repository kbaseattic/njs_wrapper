package us.kbase.narrativejobservice;

import java.util.List;
import java.util.Map;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.Tuple2;

//BEGIN_HEADER
import java.io.File;
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

import org.ini4j.Ini;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.JacksonTupleModule;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.common.taskqueue2.JobStatuses;
import us.kbase.common.taskqueue2.RestartChecker;
import us.kbase.common.taskqueue2.TaskQueue;
import us.kbase.common.taskqueue2.TaskQueueConfig;
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

    //BEGIN_CLASS_HEADER
    public static final String SYS_PROP_KB_DEPLOYMENT_CONFIG = "KB_DEPLOYMENT_CONFIG";
    public static final String SERVICE_DEPLOYMENT_NAME = "NarrativeJobService";
    
    public static final String CFG_PROP_SCRATCH = "scratch";
    public static final String CFG_PROP_WORKSPACE_SRV_URL = "workspace.srv.url";
    public static final String CFG_PROP_JOBSTATUS_SRV_URL = "jobstatus.srv.url";
    public static final String CFG_PROP_QUEUE_DB_DIR = "queue.db.dir";
    public static final String CFG_PROP_THREAD_COUNT = "thread.count";
    public static final String CFG_PROP_NJS_SRV_URL = "njs.srv.url";
    public static final String CFG_PROP_REBOOT_MODE = "reboot.mode";
    public static final String CFG_PROP_RUNNING_TASKS_PER_USER = "running.tasks.per.user";
    public static final String CFG_PROP_ADMIN_USER_NAME = "admin.user";
    public static final String CFG_PROP_SHOCK_URL = "shock.url";
    public static final String CFG_PROP_AWE_SRV_URL = "awe.srv.url";
    public static final String CFG_PROP_MAX_JOB_SIZE = "max.job.size";
    public static final String CFG_PROP_AWE_CLIENT_SCRATCH = "awe.client.scratch";
    public static final String CFG_PROP_AWE_CLIENT_DOCKER_URI = "awe.client.docker.uri";
    public static final String CFG_PROP_DOCKER_REGISTRY_URL = "docker.registry.url";
    public static final String AWE_CLIENT_SCRIPT_NAME = "run_async_srv_method.sh";
    public static final String CFG_PROP_CATALOG_SRV_URL = "catalog.srv.url";
    public static final String CFG_PROP_CATALOG_ADMIN_USER = "catalog.admin.user";
    public static final String CFG_PROP_CATALOG_ADMIN_PWD = "catalog.admin.pwd";
    public static final String CFG_PROP_KBASE_ENDPOINT = "kbase.endpoint";
    public static final String CFG_PROP_SELF_EXTERNAL_URL = "self.external.url";
    public static final String CFG_PROP_REF_DATA_BASE = "ref.data.base";
    
    public static final String VERSION = "0.2.2";
    
    public static final String AWE_APPS_TABLE_NAME = "awe_apps";
    public static final String AWE_TASK_TABLE_NAME = "awe_tasks";
    public static final String AWE_LOGS_TABLE_NAME = "awe_logs";
    
    private static Throwable configError = null;
    private static String configPath = null;
    private static Map<String, String> config = null;
    
    private static TaskQueue taskHolder = null;
    private static TaskQueueConfig taskConfig = null;
    
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
    	if (ret == null)
    		throw new IllegalStateException("Parameter " + CFG_PROP_QUEUE_DB_DIR + " is not defined in configuration");
    	File dir = new File(ret);
    	if (!dir.exists())
    		dir.mkdirs();
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

    public static synchronized TaskQueue getTaskQueue() throws Exception {
    	if (taskHolder == null) {
    		TaskQueueConfig cfg = getTaskConfig();
			taskHolder = new TaskQueue(cfg, new RestartChecker() {
				@Override
				public boolean isInRestartMode() {
					return getRebootMode();
				}
			}, new RunAppBuilder());
			System.out.println("Initial queue size: " + TaskQueue.getDbConnection(cfg.getQueueDbDir()).collect(
					"select count(*) from " + TaskQueue.QUEUE_TABLE_NAME, new us.kbase.common.utils.DbConn.SqlLoader<Integer>() {
				public Integer collectRow(java.sql.ResultSet rs) throws java.sql.SQLException { return rs.getInt(1); }
			}));
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
            String rpcName = rpcCallData.getMethod();
            List<UObject> paramsList = rpcCallData.getParams();
            List<Object> result = null;
            String errorMessage = null;
            ObjectMapper mapper = new ObjectMapper().registerModule(new JacksonTupleModule());
            try {
                if (rpcName.endsWith("_async")) {
                    String origRpcName = rpcName.substring(0, rpcName.lastIndexOf('_'));
                    RunJobParams runJobParams = new RunJobParams();
                    String serviceVer = rpcCallData.getContext() == null ? null : 
                        (String)rpcCallData.getContext().getAdditionalProperties().get("service_ver");
                    runJobParams.setServiceVer(serviceVer);
                    runJobParams.setMethod(origRpcName);
                    runJobParams.setParams(paramsList);
                    runJobParams.setRpcContext(UObject.transformObjectToObject(rpcCallData.getContext(), RpcContext.class));
                    result = new ArrayList<Object>(); 
                    result.add(runJob(runJobParams, new AuthToken(token)));
                } else if (rpcName.endsWith("_check") && paramsList.size() == 1) {
                    String jobId = paramsList.get(0).asClassInstance(String.class);
                    JobState jobState = checkJob(jobId, new AuthToken(token));
                    Long finished = jobState.getFinished();
                    if (finished != 0L) {
                        Object error = jobState.getError();
                        if (error != null) {
                            Map<String, Object> ret = new LinkedHashMap<String, Object>();
                            ret.put("version", "1.1");
                            ret.put("error", error);
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            mapper.writeValue(new UnclosableOutputStream(output), ret);
                            return;
                        }
                    }
                    result = new ArrayList<Object>();
                    result.add(jobState);
                } else {
                    errorMessage = "Method [" + rpcName + "] doesn't ends with \"_async\" or \"_check\" suffix";
                }
                if (errorMessage == null && result != null) {
                    Map<String, Object> ret = new LinkedHashMap<String, Object>();
                    ret.put("version", "1.1");
                    ret.put("result", result);
                    mapper.writeValue(new UnclosableOutputStream(output), ret);
                    return;
                } else if (errorMessage == null) {
                    errorMessage = "Unknown server error";
                }
            } catch (Exception ex) {
                errorMessage = ex.getMessage();
            }
            try {
                Map<String, Object> error = new LinkedHashMap<String, Object>();
                error.put("name", "JSONRPCError");
                error.put("code", -32601);
                error.put("message", errorMessage);
                error.put("error", errorMessage);
                Map<String, Object> ret = new LinkedHashMap<String, Object>();
                ret.put("version", "1.1");
                ret.put("error", error);
                mapper.writeValue(new UnclosableOutputStream(output), ret);
            } catch (Exception ex) {
                new Exception("Error sending error: " + errorMessage, ex).printStackTrace();
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
    @JsonServerMethod(rpc = "NarrativeJobService.run_app")
    public AppState runApp(App app, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.check_app_state")
    public AppState checkAppState(String jobId, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.suspend_app")
    public String suspendApp(String jobId, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.resume_app")
    public String resumeApp(String jobId, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.delete_app")
    public String deleteApp(String jobId, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.list_config", authOptional=true)
    public Map<String,String> listConfig(AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.ver")
    public String ver() throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.status")
    public Status status() throws Exception {
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
        //END status
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: list_running_apps</p>
     * <pre>
     * </pre>
     * @return   instance of list of type {@link us.kbase.narrativejobservice.AppState AppState} (original type "app_state")
     */
    @JsonServerMethod(rpc = "NarrativeJobService.list_running_apps", authOptional=true)
    public List<AppState> listRunningApps(AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.run_job")
    public String runJob(RunJobParams params, AuthToken authPart) throws Exception {
        String returnVal = null;
        //BEGIN run_job
        returnVal = RunAppBuilder.runAweDockerScript(params, authPart.toString(), null, config());
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
    @JsonServerMethod(rpc = "NarrativeJobService.get_job_params", tuple = true)
    public Tuple2<RunJobParams, Map<String,String>> getJobParams(String jobId, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.add_job_logs")
    public Long addJobLogs(String jobId, List<LogLine> lines, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.get_job_logs")
    public GetJobLogsResults getJobLogs(GetJobLogsParams params, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.finish_job")
    public void finishJob(String jobId, FinishJobParams params, AuthToken authPart) throws Exception {
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
    @JsonServerMethod(rpc = "NarrativeJobService.check_job")
    public JobState checkJob(String jobId, AuthToken authPart) throws Exception {
        JobState returnVal = null;
        //BEGIN check_job
        returnVal = RunAppBuilder.checkJob(jobId, authPart.toString(), config());
        //END check_job
        return returnVal;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <program> <server_port>");
            return;
        }
        new NarrativeJobServiceServer().startupServer(Integer.parseInt(args[0]));
    }
}
