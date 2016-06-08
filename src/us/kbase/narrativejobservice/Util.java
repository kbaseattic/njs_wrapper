package us.kbase.narrativejobservice;

import java.net.URL;
import java.util.UUID;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.Tuple7;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class Util {
	
	// used in RemoteAppTester & RunAppTest test classes
	public static void waitForJob(String token, String ujsUrl, String jobId) throws Exception {
		UserAndJobStateClient jscl = new UserAndJobStateClient(new URL(ujsUrl), new AuthToken(token));
		jscl.setAllSSLCertificatesTrusted(true);
		jscl.setIsInsecureHttpConnectionAllowed(true);
		for (@SuppressWarnings("unused")
		int iter = 0; ; iter++) {
			Thread.sleep(5000);
			Tuple7<String, String, String, Long, String, Long, Long> data = jscl.getJobStatus(jobId);
			Long complete = data.getE6();
			Long wasError = data.getE7();
			//System.out.println("Status (" + iter + "): " + data.getE3());
			if (complete == 1L) {
				if (wasError == 0L) {
					break;
				} else {
					String error = jscl.getDetailedError(jobId);
					//System.out.println("Detailed error:");
					//System.out.println(error);
					throw new IllegalStateException(error);
				}
			}
		}
	}
	
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
