package us.kbase.narrativejobservice;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.UObject;

public class RunAppBuilder extends DefaultTaskBuilder<String> {
	public static boolean debug = false;
	public static final String APP_STATE_QUEUED = "queued";
	public static final String APP_STATE_STARTED = "in-progress";
	public static final String APP_STATE_DONE = "completed";
	public static final String APP_STATE_ERROR = "suspend";
	public static final int MAX_HOURS_FOR_NJS_STEP = 24;

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
		appState.setJobState(APP_STATE_STARTED);
		AuthToken auth = new AuthToken(token);
		for (Step step : app.getSteps()) {
	        appState.setRunningStepId(step.getStepId());
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
	        	Util.waitForJob(token, ujsUrl, stepJobId);
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
}
