package us.kbase.njsmock.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.Tuple7;
import us.kbase.common.taskqueue.JobStatuses;
import us.kbase.common.taskqueue.TaskQueue;
import us.kbase.common.taskqueue.TaskQueueConfig;
import us.kbase.njsmock.App;
import us.kbase.njsmock.NJSMockServer;
import us.kbase.njsmock.RunAppBuilder;
import us.kbase.njsmock.Util;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;

public class RunAppTest {
	private static final String wsUrl = "https://kbase.us/services/ws";
	private static final String ujsUrl = "https://kbase.us/services/userandjobstate/";
	private static final String tempDir = "temp_files";
	
	private static TaskQueue taskHolder = null;
	
	@Test
	public void mainTest() throws Exception {
		String token = token(props(new File("test.cfg")));
		String jobId = taskHolder.addTask(new App(), token);
		Util.waitForJob(token, ujsUrl, jobId);
	}

	@AfterClass
	public static void finish() throws Exception {
		taskHolder.stopAllThreads();
	}
	
	@BeforeClass
	public static void prepare() throws Exception {
		Map<String, String> allConfigProps = new LinkedHashMap<String, String>();
		allConfigProps.put(NJSMockServer.CFG_PROP_SCRATCH, tempDir);
		JobStatuses jobStatuses = new JobStatuses() {
			@Override
			public String createAndStartJob(String token, String status, String desc,
					String initProgressPtype, String estComplete) throws Exception {
				return NJSMockServer.createJobClient(ujsUrl, token).createAndStartJob(token, status, desc, 
						new InitProgress().withPtype(initProgressPtype), estComplete);
			}
			@Override
			public void updateJob(String job, String token, String status,
					String estComplete) throws Exception {
				NJSMockServer.createJobClient(ujsUrl, token).updateJob(job, token, status, estComplete);
			}
			@Override
			public void completeJob(String job, String token, String status,
					String error, String wsUrl, String outRef) throws Exception {
				List<String> refs = new ArrayList<String>();
				if (outRef != null)
					refs.add(outRef);
				NJSMockServer.createJobClient(ujsUrl, token).completeJob(job, token, status, error, 
						new Results().withWorkspaceurl(wsUrl).withWorkspaceids(refs));
			}
		};
		File queueDir = new File(tempDir, "queuedb");
		if (queueDir.exists()) {
			deleteRecursively(queueDir);
		}
		TaskQueueConfig cfg = new TaskQueueConfig(1, queueDir, jobStatuses, wsUrl, allConfigProps);
		taskHolder = new TaskQueue(cfg, new RunAppBuilder());
		System.out.println("Initial queue size: " + TaskQueue.getDbConnection(cfg.getQueueDbDir()).collect(
				"select count(*) from " + TaskQueue.QUEUE_TABLE_NAME, new us.kbase.common.utils.DbConn.SqlLoader<Integer>() {
			public Integer collectRow(java.sql.ResultSet rs) throws java.sql.SQLException { return rs.getInt(1); }
		}));
	}
	
	public static void deleteRecursively(File fileOrDir) {
		if (fileOrDir.isDirectory())
			for (File f : fileOrDir.listFiles()) 
				deleteRecursively(f);
		fileOrDir.delete();
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
