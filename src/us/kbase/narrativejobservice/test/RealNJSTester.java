package us.kbase.narrativejobservice.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;

import com.fasterxml.jackson.core.type.TypeReference;

public class RealNJSTester {
	private static final String srvUrl = "http://140.221.66.246:7080";
	
	public static void main(String[] args) throws Exception {
		String jobId = "1a1bcd8e-83f2-4a20-a0f2-6a370f374d07";
		String token = token(props(new File("test.cfg")));
		JsonClientCaller caller = new JsonClientCaller(new URL(srvUrl), new AuthToken(token));
        caller.setInsecureHttpConnectionAllowed(true);
        caller.setAllSSLCertificatesTrusted(true);
        List<Object> input = new ArrayList<Object>(Arrays.asList(jobId));
        TypeReference<List<Object>> retType = new TypeReference<List<Object>>() {};
        Object res = caller.jsonrpcCall("NarrativeJobService.check_app_state", input, retType, true, true).get(0);
        System.out.println(res);
	}
	
	private static String token(Properties props) throws Exception {
		return AuthService.login(get(props, "user"), get(props, "password")).getToken().toString();
	}

	private static String get(Properties props, String propName) {
		String ret = props.getProperty(propName);
		if (ret == null)
			throw new IllegalStateException("Property is not defined: " + propName);
		return ret;
	}

	private static Properties props(File configFile)
			throws FileNotFoundException, IOException {
		Properties props = new Properties();
		InputStream is = new FileInputStream(configFile);
		props.load(is);
		is.close();
		return props;
	}
}
