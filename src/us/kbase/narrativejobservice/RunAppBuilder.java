package us.kbase.narrativejobservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import us.kbase.auth.AuthToken;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.LogExecStatsParams;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.ModuleVersionInfo;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.common.taskqueue2.TaskQueue;
import us.kbase.common.utils.AweUtils;
import us.kbase.common.utils.DbConn;
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
    public static final long MAX_APP_SIZE = 30000;
    public static final Set<String> asyncVersionTags = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList("dev", "beta", "release")));
    public static final int ERROR_HEAD_TAIL_LOG_LINES = 100;
    public static final int MAX_LOG_LINE_LENGTH = 10000;

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
	        AppState appState = initAppState(jobId, config);
			try {
				runApp(token, app, appState, jobId, outRef);
			} catch (Exception ex) {
			    String defaultStepId = app.getSteps().size() > 0 ? app.getSteps().get(0).getStepId() : "nostepid";
		        registerAppError(appState, ex, defaultStepId, config);
				throw ex;
			}
		} else {		// STEP
			Step step = UObject.transformStringToObject(json, Step.class);
			runStep(token, step, jobId, outRef);
		}
	}

    private static void registerAppError(AppState appState, Exception ex,
            String defaultStepId, Map<String, String> config) {
        appState.setJobState(APP_STATE_ERROR);
        String stepId = appState.getRunningStepId();
        if (stepId == null) {
        	stepId = defaultStepId;
        	appState.setRunningStepId(stepId);
        }
        String prevError = appState.getStepErrors().get(stepId);
        if (prevError == null) {
            String message = null;
            if (ex instanceof ServerException) { 
                message = ((ServerException)ex).getData();
            } 
            if (message == null) {
                message = ex.getMessage();
            }
        	appState.getStepErrors().put(stepId, message);
        }
        try {
            updateAppState(appState, config);
        } catch (Exception ignore) {}
    }

	public static synchronized AppState initAppState(String appJobId, Map<String, String> config) 
	        throws Exception {
	    AppState appState = loadAppState(appJobId, config);
	    if (appState != null)
	        return appState;
	    appState = new AppState().withStepErrors(new LinkedHashMap<String, String>())
	            .withStepOutputs(new LinkedHashMap<String, String>())
	            .withStepJobIds(new LinkedHashMap<String, String>()).withIsDeleted(0L)
	            .withJobId(appJobId).withJobState(RunAppBuilder.APP_STATE_QUEUED);
	    long startTime = System.currentTimeMillis();
	    appState.getAdditionalProperties().put("start_timestamp", startTime);
	    DbConn conn = getDbConnection(config);
	    conn.exec("insert into " + NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + " " +
	    		"(app_job_id,app_job_state,app_state_data,creation_time, modification_time) " +
	    		"values (?,?,?,?,?)", appJobId, appState.getJobState(),
	    		UObject.getMapper().writeValueAsString(appState), startTime, startTime);
	    return appState;
	}

	public static synchronized AppState loadAppState(String appJobId, Map<String, String> config)
	        throws Exception {
	    DbConn conn = getDbConnection(config);
	    List<AppState> ret = conn.collect("select app_state_data from " + 
	            NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + " where app_job_id=?", 
	            new DbConn.SqlLoader<AppState>() {
	        @Override
	        public AppState collectRow(ResultSet rs) throws SQLException {
	            try {
	                return UObject.getMapper().readValue(rs.getString(1), AppState.class);
	            } catch (IOException ex) {
	                throw new IllegalStateException(ex);
	            }
	        }
	    }, appJobId);
	    return ret.size() > 0 ? ret.get(0) : null;
	}

	public static synchronized void updateAppState(AppState appState,
	        Map<String, String> config) throws Exception {
	    String appData = UObject.getMapper().writeValueAsString(appState);
	    if (appData.length() > MAX_APP_SIZE)
	        throw new IllegalStateException("App data is too large (>" + MAX_APP_SIZE + ")");
	    DbConn conn = getDbConnection(config);
	    conn.exec("update " + NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + " set " +
	    		"app_job_state=?, app_state_data=?, modification_time=? where " +
	    		"app_job_id=?", appState.getJobState(), appData, 
	    		System.currentTimeMillis(), appState.getJobId());
	}
	
	public static synchronized List<AppState> listRunningApps(Map<String, String> config) 
	        throws Exception {
        DbConn conn = getDbConnection(config);
        List<AppState> ret = conn.collect("select app_state_data from " +
                NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + " where app_job_state=?", 
                new DbConn.SqlLoader<AppState>() {
            @Override
            public AppState collectRow(ResultSet rs)
                    throws SQLException {
                try {
                    return UObject.getMapper().readValue(rs.getString(1), AppState.class);
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }, RunAppBuilder.APP_STATE_STARTED);
        return ret;
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
		appState.setOriginalApp(app);
		appState.setJobState(APP_STATE_STARTED);
		updateAppState(appState, config);
		AuthToken auth = new AuthToken(token);
		for (Step step : app.getSteps()) {
	        appState.setRunningStepId(step.getStepId());
	        updateAppState(appState, config);
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
			if (step.getIsLongRunning() != null && step.getIsLongRunning() == 1L &&
			        srvUrl.isEmpty()) {
			    RunJobParams params = new RunJobParams().withMethod(srvMethod)
			            .withParams(step.getInputValues()).withServiceVer(step.getService().getServiceVersion());
			    String stepJobId = runAweDockerScript(params, token, jobId, config);
			    appState.getStepJobIds().put(step.getStepId(), stepJobId);
	            updateAppState(appState, config);
			    JobState jobState = null;
			    while (true) {
			        Thread.sleep(5000);
			        jobState = checkJob(stepJobId, token, config);
			        if (jobState.getFinished() != null && jobState.getFinished() == 1L)
			            break;
			    }
			    if (jobState.getError() != null) {
			        JsonRpcError retError = jobState.getError();
	                throw new ServerException(retError.getMessage(), retError.getCode().intValue(), 
	                        retError.getName(), retError.getError());
			    }
                appState.getStepOutputs().put(step.getStepId(), UObject.transformObjectToString(jobState.getResult()));
			} else {
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
		             appState.getStepJobIds().put(step.getStepId(), stepJobId);
		                updateAppState(appState, config);
			        if (debug)
			            System.out.println("Before waiting for job for app [" + jobId + "] step [" + step.getStepId() + "]");
			        waitForJob(token, ujsUrl, stepJobId, appState);
			        if (debug)
			            System.out.println("After waiting for job for app [" + jobId + "] step [" + step.getStepId() + "]");
			    }
			    appState.getStepOutputs().put(step.getStepId(), UObject.transformObjectToString(stepOutput));
			}
	        updateAppState(appState, config);			
		}
        appState.setRunningStepId(null);
        appState.setJobState(APP_STATE_DONE);
        updateAppState(appState, config);
		if (debug)
			System.out.println("End of app [" + jobId + "]");
	}
	
	public static AppState tryToRunAsOneStepAweScript(String token, App app, 
	        Map<String, String> config) throws Exception {
        if (app.getSteps().size() != 1)
            return null;
        Step step = app.getSteps().get(0);
        String srvUrl = step.getService().getServiceUrl();
        if (step.getIsLongRunning() == null || step.getIsLongRunning() != 1L ||
                !srvUrl.isEmpty())
            return null;
        String srvName = step.getService().getServiceName();
        String srvMethod = step.getService().getMethodName();
        if (srvName != null && !srvName.isEmpty())
            srvMethod = srvName + "." + srvMethod;
        RunJobParams params = new RunJobParams().withMethod(srvMethod)
                .withParams(step.getInputValues()).withServiceVer(step.getService().getServiceVersion());
        String jobId = runAweDockerScript(params, token, "", config);
        AppState appState = initAppState(jobId, config);
        appState.setOriginalApp(app);
        appState.setJobState(APP_STATE_QUEUED);
        appState.setRunningStepId(step.getStepId());
        appState.getStepJobIds().put(step.getStepId(), jobId);
        updateAppState(appState, config);
        return appState;
	}
	
	public static void checkIfAppStateNeedsUpdate(String token, AppState appState, 
            Map<String, String> config) throws Exception {
	    if (appState.getJobState().equals(APP_STATE_DONE) ||
	            appState.getJobState().equals(APP_STATE_ERROR))
	        return;
	    if (appState.getStepJobIds().size() != 1)
	        return;
	    String stepId = appState.getStepJobIds().keySet().iterator().next();
	    String stepJobId = appState.getStepJobIds().get(stepId);
	    if (!appState.getJobId().equals(stepJobId))
	        return;
	    try {
	        JobState jobState = checkJob(stepJobId, token, config);
	        StepStats stst = new StepStats();
	        stst.withCreationTime(jobState.getCreationTime());
	        stst.withExecStartTime(jobState.getExecStartTime());
	        stst.withFinishTime(jobState.getFinishTime());
	        String aweJobId = (String)jobState.getAdditionalProperties().get("awe_job_id");
	        if (aweJobId != null)
	            stst.getAdditionalProperties().put("awe_job_id", aweJobId);
            Long oldPos = appState.getPosition();
            Long newPos = jobState.getPosition();
            boolean needToUpdate = false;
            if ((oldPos == null || newPos == null || !oldPos.equals(newPos)) &&
                    (oldPos != null || newPos != null)) {
                appState.setPosition(newPos);
                stst.setPosInQueue(newPos);
                needToUpdate = true;
            }
	        StepStats oldStst = appState.getStepStats() == null ? null : appState.getStepStats().get(stepId);
	        if (oldStst == null || !oldStst.toString().equals(stst.toString())) {
	            if (appState.getStepStats() == null)
	                appState.setStepStats(new LinkedHashMap<String, StepStats>());
	            appState.getStepStats().put(stepId, stst);
	            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
	            format.setTimeZone(TimeZone.getTimeZone("UTC"));
	            if (stst.getCreationTime() != null)
	                appState.setSubmitTime(format.format(new Date(stst.getCreationTime())));
	            if (stst.getExecStartTime() != null)
	                appState.setStartTime(format.format(new Date(stst.getExecStartTime())));
	            if (stst.getFinishTime() != null)
	                appState.setCompleteTime(format.format(new Date(stst.getFinishTime())));
	            needToUpdate = true;
	        }
	        if (needToUpdate) 
	            updateAppState(appState, config);
	        if (jobState.getFinished() != null && jobState.getFinished() == 1L) {
	            if (jobState.getError() != null) {
	                JsonRpcError retError = jobState.getError();
	                String errorText = retError.getError();
	                if (errorText == null)
	                    errorText = "Message: " + retError.getMessage();
	                List<LogLine> logLines = getAweDockerScriptLogs(stepJobId, null, token, null, config).getLines();
	                if (logLines.size() > 0) {
	                    StringBuilder logPart = new StringBuilder("\nConsole output/error logs:\n");
	                    for (int i = 0; i < Math.min(ERROR_HEAD_TAIL_LOG_LINES, logLines.size()); i++)
	                        logPart.append(logLines.get(i).getLine()).append("\n");
	                    if (logLines.size() > 2 * ERROR_HEAD_TAIL_LOG_LINES)
	                        logPart.append("<<<--- " + (logLines.size() - 2 * ERROR_HEAD_TAIL_LOG_LINES) + " line(s) skipped --->>>\n");
	                    for (int i = Math.max(ERROR_HEAD_TAIL_LOG_LINES, logLines.size() - ERROR_HEAD_TAIL_LOG_LINES); i < logLines.size(); i++)
                            logPart.append(logLines.get(i).getLine()).append("\n");
	                    errorText += logPart.toString();
	                }
	                throw new ServerException(retError.getMessage(), retError.getCode().intValue(), 
	                        retError.getName(), errorText);
	            } else {
	                appState.getStepOutputs().put(stepId, UObject.transformObjectToString(jobState.getResult()));
	                updateAppState(appState, config);
                    appState.setRunningStepId(null);
                    appState.setJobState(APP_STATE_DONE);
                    updateAppState(appState, config);
	            }
	        } else if (jobState.getStatus() != null) {
	            Tuple7<String, String, String, Long, String, Long, Long> ujsStatus = 
	                    jobState.getStatus().asClassInstance(
	                            new TypeReference<Tuple7<String, String, String, Long, String, Long, Long>>() {});
	            String stage = ujsStatus.getE2();
	            if (stage != null && stage.equals("started")) {
                    appState.setJobState(APP_STATE_STARTED);
                    updateAppState(appState, config);
	            }
	        }
	    } catch (Exception ex) {
	        String defaultStepId = "nostepid";
	        registerAppError(appState, ex, defaultStepId, config);
        }
	}
	
	@SuppressWarnings("unused")
    private static void waitForJob(String token, String ujsUrl, String jobId, AppState appState) throws Exception {
		UserAndJobStateClient jscl = new UserAndJobStateClient(new URL(ujsUrl), new AuthToken(token));
		jscl.setAllSSLCertificatesTrusted(true);
		jscl.setIsInsecureHttpConnectionAllowed(true);
		for (int iter = 0; ; iter++) {
			Thread.sleep(5000);
			Tuple7<String, String, String, Long, String, Long, Long> data = jscl.getJobStatus(jobId);
    		Long complete = data.getE6();
    		Long wasError = data.getE7();
			if (complete != null && complete == 1L) {
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
            String appJobId, Map<String, String> config) throws Exception {
        if (params.getServiceVer() == null || asyncVersionTags.contains(params.getServiceVer())) {
            CatalogClient catCl = getCatalogClient(config, false);
            String moduleName = params.getMethod().split(Pattern.quote("."))[0];
            ModuleVersionInfo mvi;
            try {
                ModuleInfo mi = catCl.getModuleInfo(new SelectOneModuleParams().withModuleName(moduleName));
                String ver = params.getServiceVer();
                if (ver == null) {
                    mvi = mi.getRelease();
                } else if (ver.equals("dev")) {
                    mvi = mi.getDev();
                } else if (ver.equals("beta")) {
                    mvi = mi.getBeta();
                } else {
                    mvi = mi.getRelease();
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Error loading info from catalog for module: " + moduleName, ex);
            }
            if (mvi == null)
                throw new IllegalStateException("Cannot extract release version from catalog for module: " + moduleName);
            String serviceVer = mvi.getGitCommitHash();
            params.setServiceVer(serviceVer);
        }
        AuthToken authPart = new AuthToken(token);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        UObject.getMapper().writeValue(baos, params);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        BasicShockClient shockClient = getShockClient(authPart, config);
        String inputShockId = shockClient.addNode(bais, "job.json", "json").getId().getId();
        UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
        final String ujsJobId = ujsClient.createJob();
        String outputShockId = shockClient.addNode().getId().getId();
        String kbaseEndpoint = config.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT);
        if (kbaseEndpoint == null) {
            String wsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
            if (!wsUrl.endsWith("/ws"))
                throw new IllegalStateException("Parameter " + 
                        NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT + 
                        " is not defined in configuration");
            kbaseEndpoint = wsUrl.replace("/ws", "");
        }
        String selfExternalUrl = config.get(NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL);
        if (selfExternalUrl == null)
            selfExternalUrl = kbaseEndpoint + "/njs_wrapper";
        String aweJobId = AweUtils.runTask(getAweServerURL(config), "ExecutionEngine", params.getMethod(), 
                ujsJobId + " " + selfExternalUrl, NarrativeJobServiceServer.AWE_CLIENT_SCRIPT_NAME, 
                authPart);
        if (appJobId != null && appJobId.isEmpty())
            appJobId = ujsJobId;
        addAweTaskDescription(ujsJobId, aweJobId, inputShockId, outputShockId, appJobId, config);
        return ujsJobId;
    }
    
    public static RunJobParams getAweDockerScriptInput(String ujsJobId, String token, 
            Map<String, String> config, Map<String,String> resultConfig) throws Exception {
        updateAweTaskExecTime(ujsJobId, config, "exec_start_time");
        AuthToken authPart = new AuthToken(token);
        String inputShockId = getAweTaskInputShockId(ujsJobId, config);
        BasicShockClient shockClient = getShockClient(authPart, config);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        shockClient.getFile(new ShockNodeId(inputShockId), baos);
        baos.close();
        RunJobParams input = UObject.getMapper().readValue(baos.toByteArray(), RunJobParams.class);
        String[] propsToSend = {
                NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL, 
                NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL, 
                NarrativeJobServiceServer.CFG_PROP_SHOCK_URL,
                NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_SCRATCH, 
                NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL,
                NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI,
                NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL,
                NarrativeJobServiceServer.CFG_PROP_REF_DATA_BASE
        };
        for (String key : propsToSend) {
            String value = config.get(key);
            if (value != null)
                resultConfig.put(key, value);
        }
        String kbaseEndpoint = config.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT);
        if (kbaseEndpoint == null) {
            String wsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
            if (!wsUrl.endsWith("/ws"))
                throw new IllegalStateException("Parameter " + 
                        NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT + 
                        " is not defined in configuration");
            kbaseEndpoint = wsUrl.replace("/ws", "");
        }
        resultConfig.put(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT, kbaseEndpoint);
        String selfExternalUrl = config.get(NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL);
        if (selfExternalUrl == null)
            selfExternalUrl = kbaseEndpoint + "/njs_wrapper";
        resultConfig.put(NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL, selfExternalUrl);
        return input;
    }
    
    public static void finishAweDockerScript(String ujsJobId, FinishJobParams params, 
            String token, ErrorLogger log, Map<String, String> config) throws Exception {
        String outputShockId = getAweTaskOutputShockId(ujsJobId, config);
        String shockUrl = getShockUrl(config);
        ByteArrayInputStream bais = new ByteArrayInputStream(
                UObject.getMapper().writeValueAsBytes(params));
        updateShockNode(shockUrl, token, outputShockId, bais, "output.json", "json");
        updateAweTaskExecTime(ujsJobId, config, "finish_time");
        // let's make a call to catalog sending execution stats
        try {
            AuthToken auth = new AuthToken(token);
            String appJobId = getAweTaskAppId(ujsJobId, config);
            AppState appState = null;
            if (appJobId != null)
                appState = loadAppState(appJobId, config);
            String stepId = null;
            App app = null;
            if (appState != null && appState.getStepJobIds() != null
                    && appState.getOriginalApp() != null) {
                app = appState.getOriginalApp();
                for (String sId : appState.getStepJobIds().keySet()) {
                    if (ujsJobId.equals(appState.getStepJobIds().get(sId))) {
                        stepId = sId;
                        break;
                    }
                }
            }
            String methodSpecId = null;
            if (stepId != null && app != null && app.getSteps() != null) {
                for (Step step : app.getSteps()) {
                    if (step.getStepId().equals(stepId)) {
                        methodSpecId = step.getMethodSpecId();
                        break;
                    }
                }
            }
            String uiModuleName = null;
            if (methodSpecId != null) {
                String[] parts = methodSpecId.split("/");
                if (parts.length > 1) {
                    uiModuleName = parts[0];
                    methodSpecId = parts[1];
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BasicShockClient shockClient = getShockClient(auth, config);
            String inputShockId = getAweTaskInputShockId(ujsJobId, config);
            shockClient.getFile(new ShockNodeId(inputShockId), baos);
            baos.close();
            RunJobParams input = UObject.getMapper().readValue(baos.toByteArray(), RunJobParams.class);
            String[] parts = input.getMethod().split(Pattern.quote("."));
            String funcModuleName = parts.length > 1 ? parts[0] : null;
            String funcName = parts.length > 1 ? parts[1] : parts[0];
            String gitCommitHash = input.getServiceVer();
            Long[] execTimes = getAweTaskExecTimes(ujsJobId, config);
            long creationTime = execTimes[0];
            long execStartTime = execTimes[1];
            long finishTime = execTimes[2];
            boolean isError = params.getError() != null;
            String errorMessage = null;
            try {
                sendExecStatsToCatalog(auth.getClientId(), uiModuleName, methodSpecId, funcModuleName, 
                        funcName, gitCommitHash, creationTime, execStartTime, finishTime, isError, config);
            } catch (ServerException ex) {
                errorMessage = ex.getData();
                if (errorMessage == null)
                    errorMessage = ex.getMessage();
                if (errorMessage == null)
                    errorMessage = "Unknown server error";
            } catch (Exception ex) {
                errorMessage = ex.getMessage();
                if (errorMessage == null)
                    errorMessage = "Unknown error";
            }
            if (errorMessage != null) {
                String message = "Error sending execution stats to catalog (" + auth.getClientId() + ", " + 
                        uiModuleName + ", " + methodSpecId + ", " + funcModuleName + ", " + funcName + ", " + 
                        gitCommitHash + ", " + creationTime + ", " + execStartTime + ", " + finishTime + ", " + 
                        isError + "): " + errorMessage;
                System.err.println(message);
                if (log != null)
                    log.logErr(message);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (log != null)
                log.logErr(ex);
        }
    }
    
    public static void sendExecStatsToCatalog(String userId, String uiModuleName,
            String methodSpecId, String funcModuleName, String funcName, String gitCommitHash, 
            long creationTime, long execStartTime, long finishTime, boolean isError,
            Map<String, String> config) throws Exception {
        CatalogClient catCl = getCatalogClient(config, true);
        catCl.logExecStats(new LogExecStatsParams().withUserId(userId)
                .withAppModuleName(uiModuleName).withAppId(methodSpecId)
                .withFuncModuleName(funcModuleName).withFuncName(funcName)
                .withGitCommitHash(gitCommitHash).withCreationTime(creationTime/ 1000.0)
                .withExecStartTime(execStartTime / 1000.0).withFinishTime(finishTime / 1000.0)
                .withIsError(isError ? 1L : 0L));
    }
    
    public static int addAweDockerScriptLogs(String ujsJobId, List<LogLine> lines,
            String token, Map<String, String> config) throws Exception {
        AuthToken authPart = new AuthToken(token);
        UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
        ujsClient.getJobStatus(ujsJobId);
        DbConn conn = getDbConnection(config);
        List<Integer> r1 = conn.collect("select count(*) from " + 
                NarrativeJobServiceServer.AWE_LOGS_TABLE_NAME + " where ujs_job_id=?", 
                new DbConn.SqlLoader<Integer>() {
            @Override
            public Integer collectRow(ResultSet rs) throws SQLException {
                return rs.getInt(1);
            }
        }, ujsJobId);
        int linePos = r1.size() == 1 ? r1.get(0) : 0;
        for (LogLine line : lines) {
            String text = line.getLine();
            if (text.length() > MAX_LOG_LINE_LENGTH)
                text = text.substring(0, MAX_LOG_LINE_LENGTH - 3) + "...";
            conn.exec("insert into " + NarrativeJobServiceServer.AWE_LOGS_TABLE_NAME + 
                    " (ujs_job_id,line_pos,line,is_error) values (?,?,?,?)", 
                    ujsJobId, linePos, text, line.getIsError());
            linePos++;
        }
        return linePos;
    }
    
    public static GetJobLogsResults getAweDockerScriptLogs(String ujsJobId, Long skipLines,
            String token, Set<String> admins, Map<String, String> config) throws Exception {
        AuthToken authPart = new AuthToken(token);
        boolean isAdmin = admins != null && admins.contains(authPart.getClientId());
        if (!isAdmin) {
            // If it's not admin then let's check if there is permission in UJS
            UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
            ujsClient.getJobStatus(ujsJobId);
        }
        DbConn conn = getDbConnection(config);
        String sql = "select line,is_error from " + NarrativeJobServiceServer.AWE_LOGS_TABLE_NAME + 
                " where ujs_job_id=?" + (skipLines == null ? "" : " and line_pos>=" + skipLines) + 
                " order by line_pos";
        List<LogLine> lines = conn.collect(sql, new DbConn.SqlLoader<LogLine>() {
            @Override
            public LogLine collectRow(ResultSet rs) throws SQLException {
                return new LogLine().withLine(rs.getString(1)).withIsError(rs.getLong(2));
            }
        }, ujsJobId);
        return new GetJobLogsResults().withLines(lines)
                .withLastLineNumber((long)lines.size() + (skipLines == null ? 0L : skipLines));
    }
    
    @SuppressWarnings("unchecked")
    private static String updateShockNode(String shockUrl, String token, String shockNodeId, 
            InputStream file, final String filename, final String format) throws Exception {
        String nodeurl = shockUrl;
        if (!nodeurl.endsWith("/"))
            nodeurl += "/";
        nodeurl += "node/" + shockNodeId;
        final HttpPut htp = new HttpPut(nodeurl);
        final MultipartEntityBuilder mpeb = MultipartEntityBuilder.create();
        if (file != null) {
            mpeb.addBinaryBody("upload", file, ContentType.DEFAULT_BINARY,
                    filename);
        }
        if (format != null) {
            mpeb.addTextBody("format", format);
        }
        htp.setEntity(mpeb.build());
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(1000);
        cm.setDefaultMaxPerRoute(1000);
        CloseableHttpClient client = HttpClients.custom().setConnectionManager(cm).build();
        htp.setHeader("Authorization", "OAuth " + token);
        final CloseableHttpResponse response = client.execute(htp);
        try {
            String resp = EntityUtils.toString(response.getEntity());
            Map<String, String> node = (Map<String, String>)UObject.getMapper()
                    .readValue(resp, Map.class).get("data");
            return node.get("id");
        } finally {
            response.close();
            file.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static JobState checkJob(String jobId, String token, 
            Map<String, String> config) throws Exception {
        AuthToken authPart = new AuthToken(token);
        String ujsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        JobState returnVal = new JobState().withJobId(jobId).withUjsUrl(ujsUrl);
        String aweJobId = getAweTaskAweJobId(jobId, config);
        returnVal.getAdditionalProperties().put("awe_job_id", aweJobId);
        UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
        Tuple7<String, String, String, Long, String, Long, Long> jobStatus = 
                ujsClient.getJobStatus(jobId);
        returnVal.setStatus(new UObject(jobStatus));
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
            Map<String, Object> aweData = null;
            String aweState = null;
            String aweServerUrl = getAweServerURL(config);
            try {
                Map<String, Object> aweJob = AweUtils.getAweJobDescr(aweServerUrl, aweJobId, token);
                aweData = (Map<String, Object>)aweJob.get("data");
                if (aweData != null)
                    aweState = (String)aweData.get("state");
            } catch (Exception ex) {
                throw new IllegalStateException("Error checking AWE job (id=" + aweJobId + ") " +
                		"for ujs-id=" + jobId + " (" + ex.getMessage() + ")", ex);
            }
            if (aweState == null)
                throw new IllegalStateException("Error checking AWE job (id=" + aweJobId + ") " +
                        "for ujs-id=" + jobId + " (state is null)");
            if ((!aweState.equals("init")) && (!aweState.equals("queued")) && 
                    (!aweState.equals("in-progress"))) {
                if (aweState.equals("suspend"))
                    throw new IllegalStateException("FATAL error in AWE job (" + aweState + 
                            " for id=" + aweJobId + ")" + (jobStatus.getE2().equals("created") ? 
                                    " whereas job script wasn't started at all" : ""));
                throw new IllegalStateException("Unexpected AWE job state: " + aweState);
            }
            returnVal.getAdditionalProperties().put("awe_job_state", aweState);
            returnVal.setFinished(0L);
            String stage = jobStatus.getE2();
            if (stage != null && stage.equals("started")) {
                returnVal.setJobState(APP_STATE_STARTED);
            } else {
                returnVal.setJobState(APP_STATE_QUEUED);
                try {
                    Map<String, Object> aweResp = AweUtils.getAweJobPosition(aweServerUrl, aweJobId, token);
                    Map<String, Object> posData = (Map<String, Object>)aweResp.get("data");
                    if (posData != null && posData.containsKey("position"))
                        returnVal.setPosition(UObject.transformObjectToObject(posData.get("position"), Long.class));
                } catch (Exception ignore) {}
            }
        } else {
            FinishJobParams result = UObject.getMapper().readValue(
                    new ByteArrayInputStream(baos.toByteArray()), FinishJobParams.class);
            returnVal.setFinished(1L);
            returnVal.setResult(result.getResult());
            returnVal.setError(result.getError());
            if (result.getError() != null) {
                returnVal.setJobState(APP_STATE_ERROR);
            } else {
                returnVal.setJobState(APP_STATE_DONE);
            }
        }
        Long[] execTimes = getAweTaskExecTimes(jobId, config);
        if (execTimes != null) {
            if (execTimes[0] != null)
                returnVal.withCreationTime(execTimes[0]);
            if (execTimes[1] != null)
                returnVal.withExecStartTime(execTimes[1]);
            if (execTimes[2] != null)
                returnVal.withFinishTime(execTimes[2]);
        }
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

    private static String getShockUrl(Map<String, String> config) throws Exception {
        String shockUrl = config.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL);
        if (shockUrl == null)
            throw new IllegalStateException("Parameter '" + 
                    NarrativeJobServiceServer.CFG_PROP_SHOCK_URL +
                    "' is not defined in configuration");
        return shockUrl;
    }
    
    private static BasicShockClient getShockClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        BasicShockClient ret = new BasicShockClient(new URL(getShockUrl(config)), auth);
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

    private static CatalogClient getCatalogClient(Map<String, String> config, boolean asAdmin) throws Exception {
        String catalogUrl = getRequiredConfigParam(config, 
                NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        CatalogClient ret;
        if (asAdmin) {
            String catalogUser = getRequiredConfigParam(config, 
                    NarrativeJobServiceServer.CFG_PROP_CATALOG_ADMIN_USER);
            String catalogPwd = getRequiredConfigParam(config, 
                    NarrativeJobServiceServer.CFG_PROP_CATALOG_ADMIN_PWD);
            ret = new CatalogClient(new URL(catalogUrl), catalogUser, catalogPwd);
        } else {
            ret = new CatalogClient(new URL(catalogUrl));
        }
        ret.setIsInsecureHttpConnectionAllowed(true);
        ret.setAllSSLCertificatesTrusted(true);
        return ret;
    }

    public static String getRequiredConfigParam(Map<String, String> config, String param) {
        String ret = config.get(param);
        if (ret == null)
            throw new IllegalStateException("Parameter '" + param + 
                    "' is not defined in configuration");
        return ret;
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
                    "output_shock_id varchar(100)," +
                    "creation_time bigint," +
                    "app_job_id varchar(100)," +
                    "exec_start_time bigint," +
                    "finish_time bigint" +
                    ")");
            conn.exec("create index " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + "_app_job_id on " + 
                    NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " (app_job_id)");
        }
        if (!checkColumn(conn, NarrativeJobServiceServer.AWE_TASK_TABLE_NAME, "creation_time")) {
            System.out.println("Adding column creation_time into table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME);
            conn.exec("alter table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + 
                    " add column creation_time bigint with default " + System.currentTimeMillis());
        }
        if (!checkColumn(conn, NarrativeJobServiceServer.AWE_TASK_TABLE_NAME, "app_job_id")) {
            System.out.println("Adding column app_job_id into table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME);
            conn.exec("alter table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + 
                    " add column app_job_id varchar(100)");
            conn.exec("create index " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + "_app_job_id on " + 
                    NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " (app_job_id)");
        }
        if (!checkColumn(conn, NarrativeJobServiceServer.AWE_TASK_TABLE_NAME, "exec_start_time")) {
            System.out.println("Adding column exec_start_time into table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME);
            conn.exec("alter table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + 
                    " add column exec_start_time bigint");
        }
        if (!checkColumn(conn, NarrativeJobServiceServer.AWE_TASK_TABLE_NAME, "finish_time")) {
            System.out.println("Adding column finish_time into table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME);
            conn.exec("alter table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + 
                    " add column finish_time bigint");
        }
        if (!conn.checkTable(NarrativeJobServiceServer.AWE_LOGS_TABLE_NAME)) {
            conn.exec("create table " + NarrativeJobServiceServer.AWE_LOGS_TABLE_NAME + " (" +
            		"ujs_job_id varchar(100)," +
            		"line_pos integer," +
            		"line long varchar," +
            		"is_error smallint," +
            		"primary key (ujs_job_id, line_pos)" +
                    ")");
        }
        if (!conn.checkTable(NarrativeJobServiceServer.AWE_APPS_TABLE_NAME)) {
            conn.exec("create table " + NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + " (" +
                    "app_job_id varchar(100) primary key," +
                    "app_job_state varchar(100)," +
                    "app_state_data long varchar," +
                    "creation_time bigint," +
                    "modification_time bigint" +
                    ")");
            conn.exec("create index " + NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + "_app_job_state on " + 
                    NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + " (app_job_state)");
        }
        return conn;
    }
    
    private static boolean checkColumn(DbConn conn, String tableName, final String columnName) throws Exception {
        try {
            conn.collect("select " + columnName + " from " + tableName + " FETCH FIRST 1 ROWS ONLY", 
                    new DbConn.SqlLoader<Boolean>() {
                @Override
                public Boolean collectRow(ResultSet rs) throws SQLException {
                    return true;
                }
            });
            return true;
        } catch (SQLException ex) {
            if (ex.getMessage().toLowerCase().contains(columnName.toLowerCase()))
                return false;
            throw ex;
        }
    }

    private static void addAweTaskDescription(String ujsJobId, String aweJobId, String inputShockId, 
            String outputShockId, String appJobId, Map<String, String> config) throws Exception {
        DbConn conn = getDbConnection(config);
        conn.exec("insert into " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + 
                " (ujs_job_id,awe_job_id,input_shock_id,output_shock_id,creation_time," +
                "app_job_id) values (?,?,?,?,?,?)", 
                ujsJobId, aweJobId, inputShockId, outputShockId, System.currentTimeMillis(), appJobId);
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

    private static String getAweTaskAppId(String ujsJobId, Map<String, String> config) throws Exception {
        List<String> rows = getDbConnection(config).collect(
                "select app_job_id " +
                "from " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " " +
                "where ujs_job_id=?", new DbConn.SqlLoader<String>() {
            @Override
            public String collectRow(ResultSet rs) throws SQLException {
                return rs.getString(1);
            }
        }, ujsJobId);
        return rows.size() == 1 ? rows.get(0) : null;
    }

    public static boolean isAweTask(String ujsJobId, Map<String, String> config) throws Exception {
        List<Integer> rows = getDbConnection(config).collect(
                "select count(*) " +
                "from " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " " +
                "where ujs_job_id=?", new DbConn.SqlLoader<Integer>() {
            @Override
            public Integer collectRow(ResultSet rs) throws SQLException {
                return rs.getInt(1);
            }
        }, ujsJobId);
        return rows.size() == 1 && rows.get(0) > 0;
    }
    
    private static String getAweTaskAweJobId(String ujsJobId, Map<String, String> config) throws Exception {
        return getAweTaskDescription(ujsJobId, config)[1];
    }

    private static String getAweTaskInputShockId(String ujsJobId, Map<String, String> config) throws Exception {
        return getAweTaskDescription(ujsJobId, config)[2];
    }

    private static String getAweTaskOutputShockId(String ujsJobId, Map<String, String> config) throws Exception {
        return getAweTaskDescription(ujsJobId, config)[3];
    }
    
    private static void updateAweTaskExecTime(String ujsJobId, Map<String, String> config, String timeField) throws Exception {
        DbConn conn = getDbConnection(config);
        conn.exec("update " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " set " + timeField + "=? " +
        		"where ujs_job_id=?", System.currentTimeMillis(), ujsJobId);
    }
    
    private static Long[] getAweTaskExecTimes(String ujsJobId, Map<String, String> config) throws Exception {
        List<Long[]> rows = getDbConnection(config).collect(
                "select creation_time, exec_start_time, finish_time " +
                "from " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " " +
                "where ujs_job_id=?", new DbConn.SqlLoader<Long[]>() {
            @Override
            public Long[] collectRow(ResultSet rs) throws SQLException {
                Long[] ret = new Long[3];
                long creationTime = rs.getLong(1);
                if (!rs.wasNull())
                    ret[0] = creationTime;
                long execStartTime = rs.getLong(2);
                if (!rs.wasNull())
                    ret[1] = execStartTime;
                long finishTime = rs.getLong(3);
                if (!rs.wasNull())
                    ret[2] = finishTime;
                return ret;
            }
        }, ujsJobId);
        if (rows.size() != 1)
            return null;
        return rows.get(0);
    }
}
