package us.kbase.narrativejobservice;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class Util {
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
	
	public static boolean isAweJobId(String jobId) {
		// xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
		try {
			UUID uuid = UUID.fromString(jobId);
			return uuid.version() == 4;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}
	
	public static boolean isUjsJobId(String jobId) {
		// 545a7b6ee4b0d82af0eafa16
		return jobId.length() == 24;
	}
	
    @SuppressWarnings("unchecked")
    public static String addShockNodePublicReadACL(String shockUrl, String token, 
            String shockNodeId) throws Exception {
        String nodeurl = shockUrl;
        if (!nodeurl.endsWith("/"))
            nodeurl += "/";
        nodeurl += "node/" + shockNodeId + "/acl/public_read";
        final HttpPut htp = new HttpPut(nodeurl);
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(1000);
        cm.setDefaultMaxPerRoute(1000);
        CloseableHttpClient client = HttpClients.custom().setConnectionManager(cm).build();
        htp.setHeader("Authorization", "OAuth " + token);
        final CloseableHttpResponse response = client.execute(htp);
        try {
            String resp = EntityUtils.toString(response.getEntity());
            Map<String, String> node = (Map<String, String>)UObject.getMapper()
                    .readValue(resp, Map.class).get("data");
            return node.get("id");
        } finally {
            response.close();
        }
    }
}
