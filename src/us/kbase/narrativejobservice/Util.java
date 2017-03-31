package us.kbase.narrativejobservice;

import java.util.UUID;

public class Util {
	
	// used in the NJS *App* methods, not in SDK methods
	public static boolean isAweJobId(String jobId) {
		// xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
		try {
			UUID uuid = UUID.fromString(jobId);
			return uuid.version() == 4;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}
	
	// currently unused in the NJS wrapper codebase
	public static boolean isUjsJobId(String jobId) {
		// 545a7b6ee4b0d82af0eafa16
		return jobId.length() == 24;
	}
}
