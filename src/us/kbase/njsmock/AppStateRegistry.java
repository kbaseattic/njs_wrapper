package us.kbase.njsmock;

import java.util.HashMap;
import java.util.Map;

public class AppStateRegistry {
	private static Map<String, AppJobs> appRunIdToState = new HashMap<String, AppJobs>();
	
	public static synchronized void setAppState(String appRunId, AppJobs appState) {
		appRunIdToState.put(appRunId, appState);
	}
	
	public static synchronized AppJobs getAppState(String appRunId) {
		return appRunIdToState.get(appRunId);
	}
}
