package us.kbase.narrativejobservice;

import java.util.List;
import java.util.Map;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;

//BEGIN_HEADER
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.ini4j.Ini;

import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.JsonClientException;
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
    
    public static final String VERSION = "0.1.0";
    
    private static Throwable configError = null;
    private static String configPath = null;
    private static Map<String, String> config = null;
    
    private static TaskQueue taskHolder = null;
    private static TaskQueueConfig taskConfig = null;

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

    public static String getAdminUser() {
    	String ret = config().get(CFG_PROP_ADMIN_USER_NAME);
    	if (ret == null)
    		throw new IllegalStateException("Parameter " + CFG_PROP_ADMIN_USER_NAME + " is not defined in configuration");
    	return ret;
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
    		Map<String, String> allConfigProps = new LinkedHashMap<String, String>();
    		allConfigProps.put(CFG_PROP_SCRATCH, getTempDir().getAbsolutePath());
    		allConfigProps.put(CFG_PROP_JOBSTATUS_SRV_URL, ujsUrl);
    		allConfigProps.put(CFG_PROP_NJS_SRV_URL, getNJSServiceURL());
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
    //END_CLASS_HEADER

    public NarrativeJobServiceServer() throws Exception {
        super("NarrativeJobService");
        //BEGIN_CONSTRUCTOR
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
        	String appJobId = taskHolder.addTask(UObject.transformObjectToString(app), authPart.toString());
        	returnVal = AppStateRegistry.initAppState(appJobId);
        }
        //END run_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: check_app_state</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of String
     * @return   instance of type {@link us.kbase.narrativejobservice.AppState AppState} (original type "app_state")
     */
    @JsonServerMethod(rpc = "NarrativeJobService.check_app_state")
    public AppState checkAppState(String jobId, AuthToken authPart) throws Exception {
        AppState returnVal = null;
        //BEGIN check_app_state
        returnVal = AppStateRegistry.getAppState(jobId);
        if (returnVal == null) {
        	returnVal = getForwardClient(authPart).checkAppState(jobId);
        }
        //END check_app_state
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: suspend_app</p>
     * <pre>
     * status - 'success' or 'failure' of action
     * </pre>
     * @param   jobId   instance of String
     * @return   parameter "status" of String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.suspend_app")
    public String suspendApp(String jobId, AuthToken authPart) throws Exception {
        String returnVal = null;
        //BEGIN suspend_app
        returnVal = getForwardClient(authPart).suspendApp(jobId);
        //END suspend_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: resume_app</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of String
     * @return   parameter "status" of String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.resume_app")
    public String resumeApp(String jobId, AuthToken authPart) throws Exception {
        String returnVal = null;
        //BEGIN resume_app
        returnVal = getForwardClient(authPart).resumeApp(jobId);
        //END resume_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: delete_app</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of String
     * @return   parameter "status" of String
     */
    @JsonServerMethod(rpc = "NarrativeJobService.delete_app")
    public String deleteApp(String jobId, AuthToken authPart) throws Exception {
        String returnVal = null;
        //BEGIN delete_app
        AppState appState = AppStateRegistry.getAppState(jobId);
        if (appState == null) {
        	returnVal = getForwardClient(authPart).deleteApp(jobId);
        } else {
        	appState.setIsDeleted(1L);
        	returnVal = "App " + jobId + " was marked for deletion";
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
        if (taskHolder.getStoppingMode())
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
        int queued = taskHolder.getQueuedTasks();
        int running = taskHolder.getAllTasks() - queued;
        returnVal = new Status().withRebootMode(getRebootMode() ? 1L : 0L)
        		.withStoppingMode(taskHolder.getStoppingMode() ? 1L : 0L)
        		.withRunningTasksTotal((long)running)
        		.withRunningTasksPerUser(taskHolder.getRunningTasksPerUser())
        		.withTasksInQueue((long)queued);
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
        if (!authPart.getClientId().equals(getAdminUser()))
        	throw new IllegalStateException("Only admin of service can list internal apps");
        returnVal = AppStateRegistry.listRunningApps();
        //END list_running_apps
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
