package us.kbase.narrativejobservice;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppStateRegistry {
	private static Map<String, AppState> appJobIdToState = new HashMap<String, AppState>();
	
	public static synchronized AppState initAppState(String appJobId) {
		if (appJobIdToState.containsKey(appJobId))
			return appJobIdToState.get(appJobId);
        AppState appState = new AppState().withStepErrors(new LinkedHashMap<String, String>())
        		.withStepOutputs(new LinkedHashMap<String, String>())
        		.withJobId(appJobId).withJobState(RunAppBuilder.APP_STATE_QUEUED);
		appJobIdToState.put(appJobId, appState);
		return appState;
	}
	
	public static synchronized AppState getAppState(String appJobId) {
		return appJobIdToState.get(appJobId);
	}
}
