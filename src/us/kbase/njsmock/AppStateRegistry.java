package us.kbase.njsmock;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import us.kbase.common.service.UObject;

public class AppStateRegistry {
	private static Map<String, AppState> appJobIdToState = new HashMap<String, AppState>();
	
	public static synchronized AppState initAppState(String appJobId) {
		if (appJobIdToState.containsKey(appJobId))
			return appJobIdToState.get(appJobId);
        AppState appState = new AppState().withStepJobIds(new LinkedHashMap<String, String>())
        		.withStepOutputs(new LinkedHashMap<String, UObject>()).withAppJobId(appJobId);
		appJobIdToState.put(appJobId, appState);
		return appState;
	}
	
	public static synchronized AppState getAppState(String appJobId) {
		return appJobIdToState.get(appJobId);
	}
}
