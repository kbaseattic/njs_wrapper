package us.kbase.narrativejobservice;

import static us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner.getDb;
import static us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner.loadAppState;
import static us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner.runJob;
import static us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner.checkJob;
import static us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner.getJobLogs;
import static us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner.requestClientGroups;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.narrativejobservice.db.ExecApp;
import us.kbase.narrativejobservice.db.ExecEngineMongoDb;
import us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class RunAppBuilder extends DefaultTaskBuilder<String> {
	public static boolean debug = false;
	public static final String APP_STATE_QUEUED = SDKMethodRunner.APP_STATE_QUEUED;
	public static final String APP_STATE_STARTED = SDKMethodRunner.APP_STATE_STARTED;
	public static final String APP_STATE_DONE = SDKMethodRunner.APP_STATE_DONE;
	public static final String APP_STATE_ERROR = SDKMethodRunner.APP_STATE_ERROR;
	public static final int MAX_HOURS_FOR_NJS_STEP = 24;
	public static final long MAX_APP_SIZE = 3000000;
	public static final int ERROR_HEAD_TAIL_LOG_LINES = 100;

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
	    ExecEngineMongoDb db = getDb(config);
	    ExecApp dbApp = new ExecApp();
	    dbApp.setAppJobId(appJobId);
	    dbApp.setAppJobState(appState.getJobState());
	    dbApp.setAppStateData(UObject.getMapper().writeValueAsString(appState));
	    dbApp.setCreationTime(startTime);
	    dbApp.setModificationTime(startTime);
	    db.insertExecApp(dbApp);
	    return appState;
	}

	public static synchronized void updateAppState(AppState appState,
	        Map<String, String> config) throws Exception {
	    String appData = UObject.getMapper().writeValueAsString(appState);
	    if (appData.length() > MAX_APP_SIZE)
	        throw new IllegalStateException("App data is too large (>" + MAX_APP_SIZE + ")");
	    ExecEngineMongoDb db = getDb(config);
	    db.updateExecAppData(appState.getJobId(), appState.getJobState(), appData);
	}
	
	public static synchronized List<AppState> listRunningApps(Map<String, String> config) 
	        throws Exception {
        ExecEngineMongoDb db = getDb(config);
        List<ExecApp> dbApps = db.getExecAppsWithState(RunAppBuilder.APP_STATE_STARTED);
        List<AppState> ret = new ArrayList<AppState>();
        for (ExecApp dbApp : dbApps)
            ret.add(UObject.getMapper().readValue(dbApp.getAppStateData(), AppState.class));
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
	
	private void runApp(String token, App app, AppState appState, String jobId, 
	        String outRef) throws Exception {
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
				throw new IllegalStateException("Unsupported type for step [" + 
				        step.getStepId() + "]: " + step.getType());
			String srvUrl = step.getService().getServiceUrl();
			String srvName = step.getService().getServiceName();
			String srvMethod = step.getService().getMethodName();
			if (srvName != null && !srvName.isEmpty())
				srvMethod = srvName + "." + srvMethod;
			if (step.getIsLongRunning() != null && step.getIsLongRunning() == 1L &&
			        srvUrl.isEmpty()) {
			    RunJobParams params = new RunJobParams().withMethod(srvMethod)
			            .withParams(step.getInputValues())
			            .withServiceVer(step.getService().getServiceVersion())
			            .withAppId(step.getMethodSpecId());
			    String stepJobId = runJob(params, token, jobId, config, null);
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
                .withParams(step.getInputValues())
                .withServiceVer(step.getService().getServiceVersion())
                .withAppId(step.getMethodSpecId());
        String aweClientGroups = requestClientGroups(config, srvMethod);
        String jobId = runJob(params, token, "", config, aweClientGroups);
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
	                List<LogLine> logLines = getJobLogs(stepJobId, null, token, null, config).getLines();
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
	
	private static void waitForJob(String token, String ujsUrl, String jobId, AppState appState) throws Exception {
		UserAndJobStateClient jscl = new UserAndJobStateClient(new URL(ujsUrl), new AuthToken(token));
		jscl.setAllSSLCertificatesTrusted(true);
		jscl.setIsInsecureHttpConnectionAllowed(true);
		for (@SuppressWarnings("unused") int iter = 0; ; iter++) {
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
}
