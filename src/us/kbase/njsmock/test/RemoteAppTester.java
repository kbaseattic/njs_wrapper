package us.kbase.njsmock.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.UObject;
import us.kbase.njsmock.App;
import us.kbase.njsmock.AppState;
import us.kbase.njsmock.NJSMockClient;
import us.kbase.njsmock.Util;

public class RemoteAppTester {
	private static final String ujsUrl = "https://kbase.us/services/userandjobstate/";
	
	public static void main(String[] args) throws Exception {
		String token = token(props(new File("test.cfg")));
		App app = UObject.getMapper().readValue(RemoteAppTester.class.getResourceAsStream("app1.json.properties"), App.class);
		String appRunId = "BigApp" + System.currentTimeMillis();
		app.withAppRunId(appRunId);
		NJSMockClient cl = new NJSMockClient(new URL("http://140.221.67.204:8200/"), new AuthToken(token));
		cl.setIsInsecureHttpConnectionAllowed(true);
		AppState appState = cl.runApp(app);
		Util.waitForJob(token, ujsUrl, appState.getAppJobId());
		appState = cl.checkAppState(appRunId);
		System.out.println("Jobs: " + appState.getStepJobIds());
		System.out.println("Outputs: " + appState.getStepOutputs());
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
