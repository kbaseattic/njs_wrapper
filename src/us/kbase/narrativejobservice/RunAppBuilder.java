package us.kbase.narrativejobservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.common.taskqueue2.TaskQueue;
import us.kbase.common.utils.AweUtils;
import us.kbase.common.utils.DbConn;
import us.kbase.common.utils.TextUtils;
import us.kbase.shock.client.BasicShockClient;
import us.kbase.shock.client.ShockNodeId;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class RunAppBuilder extends DefaultTaskBuilder<String> {
	public static boolean debug = false;
	public static final String APP_STATE_QUEUED = "queued";
	public static final String APP_STATE_STARTED = "in-progress";
	public static final String APP_STATE_DONE = "completed";
	public static final String APP_STATE_ERROR = "suspend";
	public static final int MAX_HOURS_FOR_NJS_STEP = 24;

    private static DbConn conn = null;

	@Override
	public Class<String> getInputDataType() {
		return String.class;
	}
	
	@Override
	public String getOutRef(String inputData) {
		return null;
	}
	
	@Override
	public String getTaskDescription() {
		return "Narrative application runner";
	}
	
	@Override
	public void run(String token, String json, String jobId, String outRef)
			throws Exception {
		Map<String, Object> data = UObject.transformStringToObject(json, new TypeReference<Map<String, Object>>() {});
		if (data.get("step_id") == null) {		// APP
			App app = UObject.transformStringToObject(json, App.class);
	        AppState appState = AppStateRegistry.initAppState(jobId);
			try {
				runApp(token, app, appState, jobId, outRef);
			} catch (Exception ex) {
		        appState.setJobState(APP_STATE_ERROR);
		        String stepId = appState.getRunningStepId();
				if (stepId == null) {
					stepId = app.getSteps().size() > 0 ? app.getSteps().get(0).getStepId() : "nostepid";
					appState.setRunningStepId(stepId);
				}
				String prevError = appState.getStepErrors().get(stepId);
				if (prevError == null) {
					appState.getStepErrors().put(stepId, ex.getMessage());
				}
				throw ex;
			}
		} else {		// STEP
			Step step = UObject.transformStringToObject(json, Step.class);
			runStep(token, step, jobId, outRef);
		}
	}

    private NarrativeJobServiceClient getForwardClient(String token) throws Exception {
    	NarrativeJobServiceClient ret = new NarrativeJobServiceClient(new URL(getNjsUrl()), new AuthToken(token));
    	ret.setAllSSLCertificatesTrusted(true);
    	ret.setIsInsecureHttpConnectionAllowed(true);
    	return ret;
    }

	private void runStep(String token, Step step, String jobId, String outRef) throws Exception {
		App app = new App().withName("App wrapper for method " + step.getStepId()).withSteps(Arrays.asList(step));
		NarrativeJobServiceClient njs = getForwardClient(token);
		String njsJobId = njs.runApp(app).getJobId();
		long startTime = System.currentTimeMillis();
		while (true) {
			Thread.sleep(5000);
			AppState state = njs.checkAppState(njsJobId);
			String status = state.getJobState();
			if (debug)
				System.out.println("Method [" + jobId + "]: status=" + status);
			if (status != null) {
				if (status.equals(APP_STATE_DONE))
					break;
				if (status.equals(APP_STATE_ERROR)) {
					String error = state.getStepErrors().get(step.getStepId());
					if (error == null)
						error = state.getStepOutputs().get(step.getStepId());
					if (error == null)
						error = "Unknown error";
					throw new IllegalStateException(error);
				}
			}
			if (System.currentTimeMillis() - startTime > TimeUnit.HOURS.toMillis(MAX_HOURS_FOR_NJS_STEP))
				throw new IllegalStateException("Method has [" + status + "] status for longer than " + MAX_HOURS_FOR_NJS_STEP + " hours");
		}
	}
	
	private void runApp(String token, App app, AppState appState, String jobId, String outRef) throws Exception {
		if (debug)
			System.out.println("App [" + jobId + "]: " + app);
		appState.getAdditionalProperties().put("original_app", app);
		appState.setJobState(APP_STATE_STARTED);
		AuthToken auth = new AuthToken(token);
		for (Step step : app.getSteps()) {
	        appState.setRunningStepId(step.getStepId());
			if (appState.getIsDeleted() != null && appState.getIsDeleted() == 1L)
				throw new IllegalStateException("App was deleted");
			if (step.getType() == null || !step.getType().equals("service"))
				throw new IllegalStateException("Unsupported type for step [" + step.getStepId() + "]: " + 
						step.getType());
			String srvUrl = step.getService().getServiceUrl();
			String srvName = step.getService().getServiceName();
			String srvMethod = step.getService().getMethodName();
			if (srvName != null && !srvName.isEmpty())
				srvMethod = srvName + "." + srvMethod;
			List<UObject> values = step.getInputValues();
			JsonClientCaller caller = new JsonClientCaller(new URL(srvUrl), auth);
	        caller.setInsecureHttpConnectionAllowed(true);
	        caller.setAllSSLCertificatesTrusted(true);
	        List<Object> args = new ArrayList<Object>(values);
	        TypeReference<List<Object>> retType = new TypeReference<List<Object>>() {};
	        List<Object> res = null;
			if (debug)
				System.out.println("Before generic call for app [" + jobId + "] step [" + step.getStepId() + "]");
	        try {
	        	res = caller.jsonrpcCall(srvMethod, args, retType, true, true);
	        } catch (ServerException ex) {
	        	if (!ex.getMessage().equals("An unknown server error occured"))
	        		throw ex;
	        }
			if (debug)
				System.out.println("After generic call for app [" + jobId + "] step [" + step.getStepId() + "]");
	        Object stepOutput = res == null ? "" : (res.size() == 1 ? res.get(0) : res);
	        String stepJobId = null;
	        if (step.getIsLongRunning() != null && step.getIsLongRunning() == 1L) {
	        	if (step.getJobIdOutputField() == null) {
	        		if (stepOutput == null || !(stepOutput instanceof String))
	        			throw new IllegalStateException("Output of step [" + step.getStepId() + "] " +
	        					"is not a string value: " + stepOutput);
	        		stepJobId = (String)stepOutput;
	        	} else {
	        		if (!(stepOutput instanceof Map))
	        			throw new IllegalStateException("Output of step [" + step.getStepId() + "] " +
	        					"is not a mapping: " + stepOutput);
	        		@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>)stepOutput;
	        		stepJobId = (String)map.get(step.getJobIdOutputField());
	        		if (stepJobId == null)
	        			throw new IllegalStateException("Output of step [" + step.getStepId() + "] " +
	        					"doesn't contain field [" + step.getJobIdOutputField() + "]: " + stepOutput);
	        	}
	        }
	        if (stepJobId != null) {
	    		if (debug)
	    			System.out.println("Before waiting for job for app [" + jobId + "] step [" + step.getStepId() + "]");
	        	waitForJob(token, ujsUrl, stepJobId, appState);
	    		if (debug)
	    			System.out.println("After waiting for job for app [" + jobId + "] step [" + step.getStepId() + "]");
	        }
	        appState.getStepOutputs().put(step.getStepId(), UObject.transformObjectToString(stepOutput));
		}
        appState.setRunningStepId(null);
        appState.setJobState(APP_STATE_DONE);
		if (debug)
			System.out.println("End of app [" + jobId + "]");
	}
	
	private static void waitForJob(String token, String ujsUrl, String jobId, AppState appState) throws Exception {
		UserAndJobStateClient jscl = new UserAndJobStateClient(new URL(ujsUrl), new AuthToken(token));
		jscl.setAllSSLCertificatesTrusted(true);
		jscl.setIsInsecureHttpConnectionAllowed(true);
		for (int iter = 0; ; iter++) {
			Thread.sleep(5000);
			Tuple7<String, String, String, Long, String, Long, Long> data = jscl.getJobStatus(jobId);
    		Long complete = data.getE6();
    		Long wasError = data.getE7();
			if (complete == 1L) {
				if (wasError == 0L) {
					break;
				} else {
					String error = jscl.getDetailedError(jobId);
					throw new IllegalStateException(error);
				}
			}
			if (appState.getIsDeleted() != null && appState.getIsDeleted() == 1L)
				throw new IllegalStateException("App was deleted");
		}
	}
	
    public static String runAweDockerScript(RunJobParams params, String token, 
            Map<String, String> config) throws Exception {
        AuthToken authPart = new AuthToken(token);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        UObject.getMapper().writeValue(baos, params);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        BasicShockClient shockClient = getShockClient(authPart, config);
        String inputShockId = shockClient.addNode(bais, "job.json", "json").getId().getId();
        UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
        final String ujsJobId = ujsClient.createJob();
        String outputShockId = shockClient.addNode().getId().getId();
        Map<String, String> clientScriptConfig = new LinkedHashMap<String, String>();
        String[] propsToSend = {
                NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL, 
                NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL, 
                NarrativeJobServiceServer.CFG_PROP_SHOCK_URL,
                NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_SCRATCH, 
                NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL
        };
        for (String key : propsToSend) {
            String value = config.get(key);
            if (value != null)
                clientScriptConfig.put(key, value);
        }
        String aweJobId = AweUtils.runTask(getAweServerURL(config), "ExecutionEngine", 
                params.getMethod(), ujsJobId + " " + inputShockId + " " + outputShockId +
                TextUtils.stringToHex(UObject.getMapper().writeValueAsString(clientScriptConfig)), 
                NarrativeJobServiceServer.AWE_CLIENT_SCRIPT_NAME, authPart.toString());
        addAweTaskDescription(ujsJobId, aweJobId, inputShockId, outputShockId, config);
        return ujsJobId;
    }
    
    public static JobState checkJob(String jobId, String token, 
            Map<String, String> config) throws Exception {
        AuthToken authPart = new AuthToken(token);
        JobState returnVal = new JobState();
        UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
        Tuple7<String, String, String, Long, String, Long, Long> jobStatus = 
                ujsClient.getJobStatus(jobId);
        boolean complete = jobStatus.getE6() != null && jobStatus.getE6() == 1L;
        String outputShockId = getAweTaskOutputShockId(jobId, config);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (complete) {
            BasicShockClient shockClient = getShockClient(authPart, config);
            shockClient.getFile(new ShockNodeId(outputShockId), baos);
        }
        baos.close();
        if (baos.size() == 0) {
            // We should consult AWE for case the job was killed or gone with no reason.
            String aweJobId = getAweTaskAweJobId(jobId, config);
            String aweState;
            try {
                InputStream is = new URL(getAweServerURL(config) + "/job/" + 
                        aweJobId).openStream();
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> aweJob = mapper.readValue(is, Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>)aweJob.get("data");
                aweState = (String)data.get("state");
            } catch (Exception ex) {
                throw new IllegalStateException("Error checking AWE job for ujs-id=" + jobId + 
                        " (" + ex.getMessage() + ")", ex);
            }
            if ((!aweState.equals("init")) && (!aweState.equals("queued")) && 
                    (!aweState.equals("in-progress")) && (!aweState.equals("completed"))) {
                throw new IllegalStateException("Unexpected job state: " + aweState);
            }
            returnVal.setFinished(0L);
        } else {
            FinishJobParams result = UObject.getMapper().readValue(
                    new ByteArrayInputStream(baos.toByteArray()), FinishJobParams.class);
            returnVal.setFinished(1L);
            returnVal.setResult(result.getResult());
            returnVal.setError(result.getError());
        }
        //END check_job
        return returnVal;
    }
    
    private static UserAndJobStateClient getUjsClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        String jobSrvUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        if (jobSrvUrl == null)
            throw new IllegalStateException("Parameter '" + 
                    NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL + 
                    "' is not defined in configuration");
        UserAndJobStateClient ret = new UserAndJobStateClient(new URL(jobSrvUrl), auth);
        ret.setIsInsecureHttpConnectionAllowed(true);
        ret.setAllSSLCertificatesTrusted(true);
        return ret;
    }

    private static BasicShockClient getShockClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        String shockUrl = config.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL);
        if (shockUrl == null)
            throw new IllegalStateException("Parameter '" + 
                    NarrativeJobServiceServer.CFG_PROP_SHOCK_URL +
                    "' is not defined in configuration");
        BasicShockClient ret = new BasicShockClient(new URL(shockUrl), auth);
        return ret;
    }

    private static String getAweServerURL(Map<String, String> config) throws Exception {
        String aweUrl = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_SRV_URL);
        if (aweUrl == null)
            throw new IllegalStateException("Parameter '" + 
                    NarrativeJobServiceServer.CFG_PROP_AWE_SRV_URL +
                    "' is not defined in configuration");
        return aweUrl;
    }

    public static File getQueueDbDir(Map<String, String> config) {
        String ret = config.get(NarrativeJobServiceServer.CFG_PROP_QUEUE_DB_DIR);
        if (ret == null)
            throw new IllegalStateException("Parameter " + 
                    NarrativeJobServiceServer.CFG_PROP_QUEUE_DB_DIR + " is not " +
                    "defined in configuration");
        File dir = new File(ret);
        if (!dir.exists())
            dir.mkdirs();
        return dir;
    }

    private static DbConn getDbConnection(Map<String, String> config) throws Exception {
        if (conn != null)
            return conn;
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        File dbDir = new File(getQueueDbDir(config), TaskQueue.DERBY_DB_NAME);
        String url = "jdbc:derby:" + dbDir.getParent() + "/" + dbDir.getName();
        if (!dbDir.exists())
            url += ";create=true";
        conn = new DbConn(DriverManager.getConnection(url));
        if (!conn.checkTable(NarrativeJobServiceServer.AWE_TASK_TABLE_NAME)) {
            conn.exec("create table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " (" +
                    "ujs_job_id varchar(100) primary key," +
                    "awe_job_id varchar(100)," +
                    "input_shock_id varchar(100)," +
                    "output_shock_id varchar(100)" +
                    ")");
        }
        return conn;
    }

    private static void addAweTaskDescription(String ujsJobId, String aweJobId, String inputShockId, 
            String outputShockId, Map<String, String> config) throws Exception {
        DbConn conn = getDbConnection(config);
        conn.exec("insert into " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + 
                " (ujs_job_id,awe_job_id,input_shock_id,output_shock_id) values (?,?,?,?)", 
                ujsJobId, aweJobId, inputShockId, outputShockId);
    }

    private static String[] getAweTaskDescription(String ujsJobId, Map<String, String> config) throws Exception {
        List<String[]> rows = getDbConnection(config).collect(
                "select ujs_job_id,awe_job_id,input_shock_id,output_shock_id " +
                "from " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " " +
                "where ujs_job_id=?", new DbConn.SqlLoader<String[]>() {
            @Override
            public String[] collectRow(ResultSet rs) throws SQLException {
                return new String[] {rs.getString(1), rs.getString(2), 
                        rs.getString(3), rs.getString(4)};
            }
        }, ujsJobId);
        if (rows.size() != 1)
            throw new IllegalStateException("AWE task rows found in DB for jobid=" + 
                    ujsJobId + ": " + rows.size());
        return rows.get(0);
    }

    private static String getAweTaskAweJobId(String ujsJobId, Map<String, String> config) throws Exception {
        return getAweTaskDescription(ujsJobId, config)[1];
    }

    /*private static String getAweTaskInputShockId(String ujsJobId, Map<String, String> config) throws Exception {
        return getAweTaskDescription(ujsJobId, config)[2];
    }*/

    private static String getAweTaskOutputShockId(String ujsJobId, Map<String, String> config) throws Exception {
        return getAweTaskDescription(ujsJobId, config)[3];
    }
}
