package us.kbase.narrativejobservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppStateRegistry {
	private static Map<String, AppState> appJobIdToState = new HashMap<String, AppState>();
	
	public static synchronized AppState initAppState(String appJobId) {
		if (appJobIdToState.containsKey(appJobId))
			return appJobIdToState.get(appJobId);
        AppState appState = new AppState().withStepErrors(new LinkedHashMap<String, String>())
        		.withStepOutputs(new LinkedHashMap<String, String>()).withIsDeleted(0L)
        		.withJobId(appJobId).withJobState(RunAppBuilder.APP_STATE_QUEUED);
        appState.getAdditionalProperties().put("start_timestamp", System.currentTimeMillis());
		appJobIdToState.put(appJobId, appState);
		return appState;
	}
	
	public static synchronized AppState getAppState(String appJobId) {
		return appJobIdToState.get(appJobId);
	}
	
	public static synchronized List<AppState> listRunningApps() {
		List<AppState> ret = new ArrayList<AppState>();
		for (String jobId : appJobIdToState.keySet()) {
			AppState appState = appJobIdToState.get(jobId);
			if (appState.getJobState().equals(RunAppBuilder.APP_STATE_STARTED))
				ret.add(appState);
		}
		return ret;
	}
}
