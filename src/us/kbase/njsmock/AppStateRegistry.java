package us.kbase.njsmock;

import java.util.HashMap;
import java.util.Map;

public class AppStateRegistry {
	private static Map<String, AppState> appRunIdToState = new HashMap<String, AppState>();
	
	public static synchronized void setAppState(String appRunId, AppState appState) {
		appRunIdToState.put(appRunId, appState);
	}
	
	public static synchronized AppState getAppState(String appRunId) {
		return appRunIdToState.get(appRunId);
	}
}
