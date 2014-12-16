package us.kbase.narrativejobservice.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.narrativejobservice.App;
import us.kbase.narrativejobservice.AppState;
import us.kbase.narrativejobservice.NarrativeJobServiceClient;
import us.kbase.narrativejobservice.Util;

public class RemoteAppTester {
	private static final String ujsUrl = "https://kbase.us/services/userandjobstate/";
	
	public static void main(String[] args) throws Exception {
		//runApp1();
		checkJob("545a7b6ee4b0d82af0eafa16");
	}

	private static void checkJob(String jobId) throws Exception {
		String token = token(props(new File("test.cfg")));
		Util.waitForJob(token, ujsUrl, jobId);
		AppState appState = client(token).checkAppState(jobId);
		System.out.println("Outputs: " + appState.getStepOutputs());
		System.out.println("Errors: " + appState.getStepErrors());
	}
	
	private static void runApp1() throws Exception {
		String token = token(props(new File("test.cfg")));
		App app = UObject.getMapper().readValue(RemoteAppTester.class.getResourceAsStream("app1.json.properties"), App.class);
		NarrativeJobServiceClient cl = client(token);
		AppState appState = cl.runApp(app);
		Util.waitForJob(token, ujsUrl, appState.getJobId());
		appState = cl.checkAppState(appState.getJobId());
		System.out.println("Outputs: " + appState.getStepOutputs());
		System.out.println("Errors: " + appState.getStepErrors());
	}

	private static NarrativeJobServiceClient client(String token)
			throws UnauthorizedException, IOException, MalformedURLException,
			TokenFormatException {
		NarrativeJobServiceClient cl = new NarrativeJobServiceClient(new URL("http://140.221.67.204:8200/"), new AuthToken(token));
		cl.setIsInsecureHttpConnectionAllowed(true);
		return cl;
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
