package us.kbase.njsmock.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.common.taskqueue.JobStatuses;
import us.kbase.common.taskqueue.TaskQueue;
import us.kbase.common.taskqueue.TaskQueueConfig;
import us.kbase.njsmock.App;
import us.kbase.njsmock.AppState;
import us.kbase.njsmock.AppStateRegistry;
import us.kbase.njsmock.GenericServiceMethod;
import us.kbase.njsmock.NJSMockServer;
import us.kbase.njsmock.RunAppBuilder;
import us.kbase.njsmock.Step;
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
		App app = new App();
		String appRunId = "BigApp" + System.currentTimeMillis();
		app.withAppRunId(appRunId);
		//Step step0 = new Step().withStepId("temp0").withType("generic").withInputValues(new ArrayList<UObject>())
		//		.withGeneric(new GenericServiceMethod()
		//		.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
		//		.withMethodName("GenomeComparison.get_ncbi_genome_names"));
		String workspace = "nardevuser1:home";
		String genome1ncbiName = "Acetobacter pasteurianus 386B";
		String genome1obj = "Acetobacter_pasteurianus_386B.genome";
		String model1obj = "Acetobacter_pasteurianus_386B.model";
		String genome2ncbiName = "Acetobacter pasteurianus IFO 3283-01";
		String genome2obj = "Acetobacter_pasteurianus_IFO_3283_01.genome";
		String model2obj = "Acetobacter_pasteurianus_IFO_3283_01.model";
		String comparObj = "Acetobacter_pasteurianus.protcmp";
		Step step1a = new Step().withStepId("step1a").withType("generic")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("genome_name", genome1ncbiName)
						.put("out_genome_ws", workspace).put("out_genome_id", genome1obj).map)))
				.withGeneric(new GenericServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withMethodName("GenomeComparison.import_ncbi_genome"));
		Step step1b = new Step().withStepId("step1b").withType("generic")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("in_genome_ws", workspace).put("in_genome_id", genome1obj)
						.put("out_genome_ws", workspace).put("out_genome_id", genome1obj)
						.put("seed_annotation_only", 1L).map)))
				.withGeneric(new GenericServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withMethodName("GenomeComparison.annotate_genome"))
				.withIsLongRunning(1L);
		Step step1c = new Step().withStepId("step1c").withType("generic")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("workspace", workspace).put("genome", genome1obj)
						.put("model", model1obj).map)))
				.withGeneric(new GenericServiceMethod()
				.withServiceUrl("https://kbase.us/services/fba_model_services/")
				.withMethodName("fbaModelServices.genome_to_fbamodel"));
		Step step2a = new Step().withStepId("step2a").withType("generic")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("genome_name", genome2ncbiName)
						.put("out_genome_ws", workspace).put("out_genome_id", genome2obj).map)))
				.withGeneric(new GenericServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withMethodName("GenomeComparison.import_ncbi_genome"));
		Step step2b = new Step().withStepId("step2b").withType("generic")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("in_genome_ws", workspace).put("in_genome_id", genome2obj)
						.put("out_genome_ws", workspace).put("out_genome_id", genome2obj)
						.put("seed_annotation_only", 1L).map)))
				.withGeneric(new GenericServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withMethodName("GenomeComparison.annotate_genome"))
				.withIsLongRunning(1L);
		Step step2c = new Step().withStepId("step2c").withType("generic")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("workspace", workspace).put("genome", genome2obj)
						.put("model", model2obj).map)))
				.withGeneric(new GenericServiceMethod()
				.withServiceUrl("https://kbase.us/services/fba_model_services/")
				.withMethodName("fbaModelServices.genome_to_fbamodel"));
		Step step3 = new Step().withStepId("step3").withType("generic")
				.withInputValues(Arrays.asList(new UObject(new PreMap()
						.put("genome1ws", workspace).put("genome1id", genome1obj)
						.put("genome2ws", workspace).put("genome2id", genome2obj)
						.put("output_ws", workspace).put("output_id", comparObj).map)))
				.withGeneric(new GenericServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withMethodName("GenomeComparison.blast_proteomes"))
				.withIsLongRunning(1L);
		app.withSteps(Arrays.asList(step1a, step1b, step1c, step2a, step2b, step2c, step3));
        AppState appState = new AppState().withStepJobIds(new LinkedHashMap<String, String>())
        		.withStepOutputs(new LinkedHashMap<String, UObject>());
        String appJson = UObject.transformObjectToString(app);
        //System.out.println("Input app data:");
        //System.out.println(jsonToPretty(UObject.transformStringToObject(appJson, Object.class)));
        //System.out.println("------------------------------------------------------------------");
        AppStateRegistry.setAppState(appRunId, appState);
		String jobId = taskHolder.addTask(appJson, token);
        appState.withAppJobId(jobId);
		Util.waitForJob(token, ujsUrl, jobId);
		System.out.println("Jobs: " + AppStateRegistry.getAppState(appRunId).getStepJobIds());
		System.out.println("Outputs: " + AppStateRegistry.getAppState(appRunId).getStepOutputs());
	}

	private static String jsonToPretty(Object obj) throws Exception {
		ObjectMapper mpr = new ObjectMapper();
		return mpr.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
	}
	
	@AfterClass
	public static void finish() throws Exception {
		taskHolder.stopAllThreads();
	}
	
	@BeforeClass
	public static void prepare() throws Exception {
		Map<String, String> allConfigProps = new LinkedHashMap<String, String>();
		allConfigProps.put(NJSMockServer.CFG_PROP_SCRATCH, tempDir);
		allConfigProps.put(NJSMockServer.CFG_PROP_JOBSTATUS_SRV_URL, ujsUrl);
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
	
	public static class PreMap {
		Map<String, Object> map = new TreeMap<String, Object>();
		
		public PreMap put(String key, Object value) {
			map.put(key, value);
			return this;
		}
	}
}
