package us.kbase.narrativejobservice.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.narrativejobservice.App;
import us.kbase.narrativejobservice.AppState;
import us.kbase.narrativejobservice.NarrativeJobServiceClient;
import us.kbase.narrativejobservice.ScriptMethod;
import us.kbase.narrativejobservice.ServiceMethod;
import us.kbase.narrativejobservice.Step;
import us.kbase.narrativejobservice.StepParameter;
import us.kbase.narrativejobservice.Util;
import us.kbase.narrativejobservice.WorkspaceObject;
import us.kbase.narrativejobservice.test.RunAppTest.PreMap;

public class RemoteAppTester {
	private static final String ujsUrl = "https://kbase.us/services/userandjobstate/";
	//private static final String njsUrl = "http://140.221.66.246:7080/";
	private static final String njsUrl = "http://dev06.berkeley.kbase.us:8200/";
	
	public static void main(String[] args) throws Exception {
		//checkJob("545a7b6ee4b0d82af0eafa16");
		//checkApp("5492173f60b22dcad90498d4");
		//runApp(makeScriptApp());
		//runApp(makeServiceApp());
		runApp(loadAppFromResource("app2"));
	}

	private static void checkJob(String jobId) throws Exception {
		String token = token(props(new File("test.cfg")));
		Util.waitForJob(token, ujsUrl, jobId);
		AppState appState = client(token).checkAppState(jobId);
		System.out.println("Outputs: " + appState.getStepOutputs());
		System.out.println("Errors: " + appState.getStepErrors());
	}

	private static App makeScriptApp() throws Exception {
		return new App().withName("contigset_assembly").withSteps(Arrays.asList(makeScriptStep()));
	}
	
	private static void runApp(App app) throws Exception {
		String token = token(props(new File("test.cfg")));
		NarrativeJobServiceClient cl = client(token);
		AppState appState = cl.runApp(app);
		checkApp(appState.getJobId());
	}
	
	public static void checkApp(String jobId) throws Exception {
		String token = token(props(new File("test.cfg")));
		NarrativeJobServiceClient cl = client(token);
		for (int iter = 0; ; iter++) {
			AppState state = cl.checkAppState(jobId);
			String status = state.getJobState();
			System.out.println("Iteration " + iter + ", app [" + jobId + "] has state: " + status + " (" + state + ")");
			if (status.equals("completed") || status.equals("suspend")) {
				System.out.println("Outputs: " + state.getStepOutputs());
				System.out.println("Errors: " + state.getStepErrors());
				break;
			}
			Thread.sleep(5000);
		}
	}

	private static Step makeScriptStep() {
		String wsName = "nardevuser1:home";
		WorkspaceObject emptyWO = new WorkspaceObject().withWorkspaceName("").withObjectType("").withIsInput(0L);
		Step step = new Step().withStepId("contigset_assembly")
				.withService(new ServiceMethod())
				.withScript(new ScriptMethod().withServiceName("assembly")
						.withMethodName("assemble_contigset_from_reads").withHasFiles(1L))
				.withType("script")
				.withIsLongRunning(0L)
				.withParameters(Arrays.asList(
						new StepParameter().withWsObject(new WorkspaceObject().withWorkspaceName(wsName)
								.withObjectType("KBaseAssembly.AssemblyInput").withIsInput(1L)
								).withIsWorkspaceId(1L).withStepSource("").withValue("rhodo.CACIA14H1.reads").withLabel("assembly_input"),
						new StepParameter().withWsObject(emptyWO).withIsWorkspaceId(0L).withStepSource("")
								.withValue("asdfasdfa").withLabel("description"),
						new StepParameter().withWsObject(emptyWO).withIsWorkspaceId(0L).withStepSource("")
								.withValue("kiki").withLabel("recipe"),
						new StepParameter().withWsObject(new WorkspaceObject().withWorkspaceName(wsName)
								.withObjectType("KBaseGenomes.ContigSet").withIsInput(0L)
								).withIsWorkspaceId(1L).withStepSource("").withValue("contigset.8").withLabel("output_contigset")
				));
		return step;
	}
	
	private static App makeServiceApp() throws Exception {
		App app = new App().withName("test");
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
		Step step1a = new Step().withStepId("step1a").withType("service")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("genome_name", genome1ncbiName)
						.put("out_genome_ws", workspace).put("out_genome_id", genome1obj).map)))
				.withService(new ServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withServiceName("GenomeComparison")
				.withMethodName("import_ncbi_genome"));
		Step step1b = new Step().withStepId("step1b").withType("service")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("in_genome_ws", workspace).put("in_genome_id", genome1obj)
						.put("out_genome_ws", workspace).put("out_genome_id", genome1obj)
						.put("seed_annotation_only", 1L).map)))
				.withService(new ServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withServiceName("GenomeComparison")
				.withMethodName("annotate_genome"))
				.withIsLongRunning(1L);
		Step step1c = new Step().withStepId("step1c").withType("service")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("workspace", workspace).put("genome", genome1obj)
						.put("model", model1obj).map)))
				.withService(new ServiceMethod()
				.withServiceUrl("https://kbase.us/services/fba_model_services/")
				.withServiceName("fbaModelServices")
				.withMethodName("genome_to_fbamodel"));
		Step step2a = new Step().withStepId("step2a").withType("service")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("genome_name", genome2ncbiName)
						.put("out_genome_ws", workspace).put("out_genome_id", genome2obj).map)))
				.withService(new ServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withServiceName("GenomeComparison")
				.withMethodName("import_ncbi_genome"));
		Step step2b = new Step().withStepId("step2b").withType("service")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("in_genome_ws", workspace).put("in_genome_id", genome2obj)
						.put("out_genome_ws", workspace).put("out_genome_id", genome2obj)
						.put("seed_annotation_only", 1L).map)))
				.withService(new ServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withServiceName("GenomeComparison")
				.withMethodName("annotate_genome"))
				.withIsLongRunning(1L);
		Step step2c = new Step().withStepId("step2c").withType("service")
				.withInputValues(Arrays.asList(new UObject(
						new PreMap().put("workspace", workspace).put("genome", genome2obj)
						.put("model", model2obj).map)))
				.withService(new ServiceMethod()
				.withServiceUrl("https://kbase.us/services/fba_model_services/")
				.withServiceName("fbaModelServices")
				.withMethodName("genome_to_fbamodel"));
		Step step3 = new Step().withStepId("step3").withType("service")
				.withInputValues(Arrays.asList(new UObject(new PreMap()
						.put("genome1ws", workspace).put("genome1id", genome1obj)
						.put("genome2ws", workspace).put("genome2id", genome2obj)
						.put("output_ws", workspace).put("output_id", comparObj).map)))
				.withService(new ServiceMethod()
				.withServiceUrl("https://kbase.us/services/genome_comparison/jsonrpc")
				.withServiceName("GenomeComparison")
				.withMethodName("blast_proteomes"))
				.withIsLongRunning(1L);
		app.withSteps(Arrays.asList(step1a, step1b, step1c, step2a, step2b, step2c, step3));
		return app;
	}
	
	private static App loadAppFromResource(String name) throws Exception {
		return UObject.getMapper().readValue(RemoteAppTester.class.getResourceAsStream(name + ".json.properties"), App.class);
	}

	private static NarrativeJobServiceClient client(String token)
			throws UnauthorizedException, IOException, MalformedURLException,
			TokenFormatException {
		NarrativeJobServiceClient cl = new NarrativeJobServiceClient(new URL(njsUrl), new AuthToken(token));
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
