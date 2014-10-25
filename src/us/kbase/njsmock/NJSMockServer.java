package us.kbase.njsmock;

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
import java.util.Map;

import org.ini4j.Ini;

import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.common.taskqueue.JobStatuses;
import us.kbase.common.taskqueue.TaskQueue;
import us.kbase.common.taskqueue.TaskQueueConfig;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
//END_HEADER

/**
 * <p>Original spec-file module name: NJSMock</p>
 * <pre>
 * </pre>
 */
public class NJSMockServer extends JsonServerServlet {
    private static final long serialVersionUID = 1L;

    //BEGIN_CLASS_HEADER
    public static final String SYS_PROP_KB_DEPLOYMENT_CONFIG = "KB_DEPLOYMENT_CONFIG";
    public static final String SERVICE_DEPLOYMENT_NAME = "NarrativeJobService";
    
    public static final String CFG_PROP_SCRATCH = "scratch";
    public static final String CFG_PROP_WORKSPACE_SRV_URL = "workspace.srv.url";
    public static final String CFG_PROP_JOBSTATUS_SRV_URL = "jobstatus.srv.url";
    public static final String CFG_PROP_QUEUE_DB_DIR = "queue.db.dir";
    public static final String CFG_PROP_THREAD_COUNT = "thread.count";
    
    public static final String VERSION = "0.0.1";
    
    private static Throwable configError = null;
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
			System.out.println(NJSMockServer.class.getName() + ": Deployment config path was defined: " + configPath);
			try {
				config = new Ini(new File(configPath)).get(SERVICE_DEPLOYMENT_NAME);
			} catch (Throwable ex) {
				System.out.println(NJSMockServer.class.getName() + ": Error loading deployment config-file: " + ex.getMessage());
				configError = ex;
			}
		}
		if (config == null)
			throw new IllegalStateException("There was unknown error in service initialization when checking"
					+ "the configuration: is the ["+SERVICE_DEPLOYMENT_NAME+"] config group defined?");
		return config;
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
    
    public static synchronized TaskQueueConfig getTaskConfig() throws Exception {
    	if (taskConfig == null) {
    		int threadCount = getThreadCount();
    		File queueDbDir = getQueueDbDir();
    		final String wsUrl = getWorkspaceServiceURL();
    		final String ujsUrl = getUJSServiceURL();
    		Map<String, String> allConfigProps = new LinkedHashMap<String, String>();
    		allConfigProps.put(CFG_PROP_SCRATCH, getTempDir().getAbsolutePath());
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
			taskConfig = new TaskQueueConfig(threadCount, queueDbDir, jobStatuses, wsUrl, allConfigProps);
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
			taskHolder = new TaskQueue(cfg, new RunAppBuilder());
			System.out.println("Initial queue size: " + TaskQueue.getDbConnection(cfg.getQueueDbDir()).collect(
					"select count(*) from " + TaskQueue.QUEUE_TABLE_NAME, new us.kbase.common.utils.DbConn.SqlLoader<Integer>() {
				public Integer collectRow(java.sql.ResultSet rs) throws java.sql.SQLException { return rs.getInt(1); }
			}));
    	}
    	return taskHolder;
    }
    //END_CLASS_HEADER

    public NJSMockServer() throws Exception {
        super("NJSMock");
        //BEGIN_CONSTRUCTOR
        //END_CONSTRUCTOR
    }

    /**
     * <p>Original spec-file function name: run_app</p>
     * <pre>
     * </pre>
     * @param   app   instance of type {@link us.kbase.njsmock.App App} (original type "app")
     * @return   instance of type {@link us.kbase.njsmock.AppJobs AppJobs} (original type "app_jobs")
     */
    @JsonServerMethod(rpc = "NJSMock.run_app")
    public AppJobs runApp(App app, AuthToken authPart) throws Exception {
        AppJobs returnVal = null;
        //BEGIN run_app
        String appRunId = app.getAppRunId();
        AppJobs appState = new AppJobs().withStepJobIds(new LinkedHashMap<String, String>())
        		.withStepOutputs(new LinkedHashMap<String, UObject>());
        AppStateRegistry.setAppState(appRunId, appState);
        String appJobId = taskHolder.addTask(UObject.transformObjectToString(app), authPart.toString());
        appState.withAppJobId(appJobId);
        returnVal = appState;
        //END run_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: check_app_state</p>
     * <pre>
     * </pre>
     * @param   appRunId   instance of String
     * @return   instance of type {@link us.kbase.njsmock.AppJobs AppJobs} (original type "app_jobs")
     */
    @JsonServerMethod(rpc = "NJSMock.check_app_state")
    public AppJobs checkAppState(String appRunId, AuthToken authPart) throws Exception {
        AppJobs returnVal = null;
        //BEGIN check_app_state
        returnVal = AppStateRegistry.getAppState(appRunId);
        //END check_app_state
        return returnVal;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <program> <server_port>");
            return;
        }
        new NJSMockServer().startupServer(Integer.parseInt(args[0]));
    }
}
