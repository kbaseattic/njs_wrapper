package us.kbase.njsmock;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.UObject;

public class RunAppBuilder extends DefaultTaskBuilder<String> {

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
	public void run(String token, String appJson, String jobId, String outRef)
			throws Exception {
		App app = UObject.transformStringToObject(appJson, App.class);
		System.out.println("App: " + app);
		AuthToken auth = new AuthToken(token);
		for (Step step : app.getSteps()) {
			if (step.getType() == null || !step.getType().equals("generic"))
				throw new IllegalStateException("Unsupported type for step [" + step.getStepId() + "]: " + 
						step.getType());
			String srvUrl = step.getGeneric().getServiceUrl();
			String srvMethod = step.getGeneric().getMethodName();
			List<UObject> values = step.getInputValues();
			JsonClientCaller caller = new JsonClientCaller(new URL(srvUrl), auth);
	        caller.setInsecureHttpConnectionAllowed(true);
	        caller.setAllSSLCertificatesTrusted(true);
	        List<Object> args = new ArrayList<Object>(values);
	        TypeReference<List<Object>> retType = new TypeReference<List<Object>>() {};
	        List<Object> res = null;
	        System.out.println("Before generic call for step [" + step.getStepId() + "]");
	        try {
	        	res = caller.jsonrpcCall(srvMethod, args, retType, true, true);
	        } catch (ServerException ex) {
	        	if (!ex.getMessage().equals("An unknown server error occured"))
	        		throw ex;
	        }
	        System.out.println("After generic call for step [" + step.getStepId() + "]");
	        Object stepOutput = res == null ? null : (res.size() == 1 ? res.get(0) : res);
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
	        AppJobs appState = AppStateRegistry.getAppState(app.getAppRunId());
	        if (stepJobId != null)
	        	appState.getStepJobIds().put(step.getStepId(), stepJobId);
	        appState.getStepOutputs().put(step.getStepId(), new UObject(stepOutput));
	        if (stepJobId != null)
	        	Util.waitForJob(token, ujsUrl, stepJobId);
		}
        System.out.println("End of app [" + app.getAppRunId() + "]");
	}
}
