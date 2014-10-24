package us.kbase.njsmock;

import java.net.URL;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.Tuple7;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class Util {
	public static void waitForJob(String token, String ujsUrl, String jobId) throws Exception {
		UserAndJobStateClient jscl = new UserAndJobStateClient(new URL(ujsUrl), new AuthToken(token));
		jscl.setAllSSLCertificatesTrusted(true);
		jscl.setIsInsecureHttpConnectionAllowed(true);
		for (int iter = 0; ; iter++) {
			Tuple7<String, String, String, Long, String, Long, Long> data = jscl.getJobStatus(jobId);
			String status = data.getE3();
    		Long complete = data.getE6();
    		Long wasError = data.getE7();
			System.out.println("Status (" + iter + "): " + status);
			if (complete == 1L) {
				if (wasError == 0L) {
					break;
				} else {
					String error = jscl.getDetailedError(jobId);
					System.out.println("Detailed error:");
					System.out.println(error);
					throw new IllegalStateException("Job was failed: " + error);
				}
			}
			Thread.sleep(1000);
		}
	}
}
