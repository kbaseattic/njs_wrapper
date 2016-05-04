package us.kbase.narrativejobservice.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import junit.framework.Assert;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.catalog.AppClientGroup;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.GetClientGroupParams;
import us.kbase.catalog.LogExecStatsParams;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.ModuleVersionInfo;
import us.kbase.catalog.SelectModuleVersionParams;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.UObject;
import us.kbase.common.utils.ProcessHelper;
import us.kbase.narrativejobservice.App;
import us.kbase.narrativejobservice.AppState;
import us.kbase.narrativejobservice.GetJobLogsParams;
import us.kbase.narrativejobservice.JobState;
import us.kbase.narrativejobservice.LogLine;
import us.kbase.narrativejobservice.NarrativeJobServiceClient;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.narrativejobservice.RunAppBuilder;
import us.kbase.narrativejobservice.RunJobParams;
import us.kbase.narrativejobservice.ServiceMethod;
import us.kbase.narrativejobservice.Step;
import us.kbase.workspace.CreateWorkspaceParams;
import us.kbase.workspace.ObjectSaveData;
import us.kbase.workspace.SaveObjectsParams;
import us.kbase.workspace.WorkspaceClient;

@SuppressWarnings("unchecked")
public class AweClientDockerJobScriptTest {
    private static AuthToken token = null;
    private static NarrativeJobServiceClient client = null;
    private static File workDir = null;
    private static File mongoDir = null;
    //private static File shockDir = null;
    private static File aweServerDir = null;
    private static File aweClientDir = null;
    private static File njsServiceDir = null;
    private static Server catalogWrapper = null;
    private static Server njsService = null;
    private static String testWsName = null;
    private static final String testContigsetObjName = "temp_contigset.1";
    private static File refDataDir = null;
    
    private static int mongoInitWaitSeconds = 300;
    private static int aweServerInitWaitSeconds = 60;
    private static int aweClientInitWaitSeconds = 60;
    private static int njsJobWaitSeconds = 60;
    
    private static List<LogExecStatsParams> execStats = Collections.synchronizedList(
            new ArrayList<LogExecStatsParams>());

    @Test
    public void testOneJob() throws Exception {
        System.out.println("Test [testOneJob]");
        try {
            execStats.clear();
            String moduleName = "onerepotest";
            String methodName = "send_data";
            String serviceVer = lookupServiceVersion(moduleName);
            RunJobParams params = new RunJobParams().withMethod(
                    moduleName + "." + methodName).withServiceVer(serviceVer)
                    .withParams(Arrays.asList(UObject.fromJsonString(
                            "{\"genomeA\":\"myws.mygenome1\",\"genomeB\":\"myws.mygenome2\"}")));
            String jobId = client.runJob(params);
            JobState ret = null;
            for (int i = 0; i < 20; i++) {
                try {
                    ret = client.checkJob(jobId);
                    System.out.println("Job finished: " + ret.getFinished());
                    if (ret.getFinished() != null && ret.getFinished() == 1L) {
                        break;
                    }
                    Thread.sleep(5000);
                } catch (ServerException ex) {
                    System.out.println(ex.getData());
                    throw ex;
                }
            }
            Assert.assertNotNull(ret);
            String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(ret);
            Assert.assertEquals(errMsg, 1L, (long)ret.getFinished());
            Assert.assertNotNull(errMsg, ret.getResult());
            List<Map<String, Map<String, String>>> data = ret.getResult().asClassInstance(List.class);
            Assert.assertEquals(errMsg, 1, data.size());
            Map<String, String> outParams = data.get(0).get("params");
            Assert.assertNotNull(errMsg, outParams);
            Assert.assertEquals(errMsg, "myws.mygenome1", outParams.get("genomeA"));
            Assert.assertEquals(errMsg, "myws.mygenome2", outParams.get("genomeB"));
            Assert.assertEquals(1, execStats.size());
            LogExecStatsParams execLog = execStats.get(0);
            Assert.assertNull(execLog.getAppModuleName());
            Assert.assertNull(execLog.getAppId());
            Assert.assertEquals(moduleName, execLog.getFuncModuleName());
            Assert.assertEquals(methodName, execLog.getFuncName());
            Assert.assertEquals(serviceVer, execLog.getGitCommitHash());
            double queueTime = execLog.getExecStartTime() - execLog.getCreationTime();
            double execTime = execLog.getFinishTime() - execLog.getExecStartTime();
            Assert.assertTrue("" + execLog, queueTime > 0);
            Assert.assertTrue("" + execLog, execTime > 0);
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }

    @Test
    public void testApp() throws Exception {
        System.out.println("Test [testApp]");
        try {
            execStats.clear();
            String moduleName = "onerepotest";
            String methodName = "send_data";
            AppState st = runAsyncMethodAsAppAndWait(moduleName, methodName, 
                    "{\"genomeA\":\"myws.mygenome1\",\"genomeB\":\"myws.mygenome2\"}");
            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
            Assert.assertEquals(errMsg, "completed", st.getJobState());
            Assert.assertNotNull(errMsg, st.getStepOutputs());
            String step1output = st.getStepOutputs().get("step1");
            Assert.assertNotNull(errMsg, step1output);
            List<Map<String, Map<String, String>>> data = UObject.getMapper().readValue(step1output, List.class);
            Assert.assertEquals(errMsg, 1, data.size());
            Map<String, String> outParams = data.get(0).get("params");
            Assert.assertNotNull(errMsg, outParams);
            Assert.assertEquals(errMsg, "myws.mygenome1", outParams.get("genomeA"));
            Assert.assertEquals(errMsg, "myws.mygenome2", outParams.get("genomeB"));
            Assert.assertEquals(1, execStats.size());
            LogExecStatsParams execLog = execStats.get(0);
            Assert.assertEquals(moduleName, execLog.getAppModuleName());
            Assert.assertEquals(methodName, execLog.getAppId());
            Assert.assertEquals(moduleName, execLog.getFuncModuleName());
            Assert.assertEquals(methodName, execLog.getFuncName());
            Assert.assertEquals(lookupServiceVersion(moduleName), execLog.getGitCommitHash());
            double queueTime = execLog.getExecStartTime() - execLog.getCreationTime();
            double execTime = execLog.getFinishTime() - execLog.getExecStartTime();
            Assert.assertTrue("" + execLog, queueTime > 0);
            Assert.assertTrue("" + execLog, execTime > 0);
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }

    private AppState runAsyncMethodAsAppAndWait(String moduleName,
            String methodName, String... paramsJson) throws Exception,
            IOException, InvalidFileFormatException, JsonClientException,
            InterruptedException, ServerException {
        List<UObject> inputValues = new ArrayList<UObject>();
        for (String paramJson : paramsJson)
            inputValues.add(UObject.fromJsonString(paramJson));
        App app = new App().withName("fake").withSteps(Arrays.asList(new Step().withStepId("step1")
                .withType("service").withService(new ServiceMethod().withServiceUrl("")
                        .withServiceName(moduleName)
                        .withServiceVersion(lookupServiceVersion(moduleName))
                        .withMethodName(methodName))
                        .withInputValues(inputValues)
                        .withIsLongRunning(1L)
                        .withMethodSpecId(moduleName + "/" + methodName)));
        AppState st = client.runApp(app);
        String appJobId = st.getJobId();
        String stepJobId = null;
        for (int i = 0; i < 20; i++) {
            try {
                st = client.checkAppState(appJobId);
                System.out.println("App state: " + st.getJobState());
                if (st.getJobState().equals("queued"))
                    Assert.assertNotNull(st.getPosition());
                stepJobId = st.getStepJobIds().get("step1");
                if (stepJobId != null)
                    System.out.println("Step finished: " + client.checkJob(stepJobId).getFinished());
                if (st.getJobState().equals(RunAppBuilder.APP_STATE_DONE) ||
                        st.getJobState().equals(RunAppBuilder.APP_STATE_ERROR)) {
                    break;
                }
                Thread.sleep(5000);
            } catch (ServerException ex) {
                System.out.println(ex.getData());
                throw ex;
            }
        }
        Assert.assertNotNull(stepJobId);
        return st;
    }
    
    @Test
    public void testLogging() throws Exception {
        System.out.println("Test [testLogging]");
        try {
            execStats.clear();
            String moduleName = "onerepotest";
            String methodName = "print_lines";
            String serviceVer = lookupServiceVersion(moduleName);
            App app = new App().withName("fake").withSteps(Arrays.asList(
                    new Step().withStepId("step1").withType("service").withService(
                            new ServiceMethod().withServiceUrl("")
                            .withServiceName(moduleName)
                            .withServiceVersion(serviceVer)
                            .withMethodName("send_data"))
                            .withInputValues(Arrays.asList(UObject.fromJsonString(
                                    "{\"genomeA\":\"myws.mygenome1\",\"genomeB\":\"myws.mygenome2\"}")))
                                    .withIsLongRunning(1L),
                    new Step().withStepId("step2").withType("service").withService(
                            new ServiceMethod().withServiceUrl("")
                            .withServiceName(moduleName)
                            .withServiceVersion(serviceVer)
                            .withMethodName(methodName))
                            .withInputValues(Arrays.asList(UObject.fromJsonString(
                                    "\"First line\\nSecond super long line\\nshort\"")))
                                    .withIsLongRunning(1L)
                                    .withMethodSpecId(moduleName + "/" + methodName)
                                    ));
            AppState st = client.runApp(app);
            String appJobId = st.getJobId();
            String stepJobId = null;
            JobState ret = null;
            int logLinesRecieved = 0;
            int numberOfOneLiners = 0;
            for (int i = 0; i < 30; i++) {
                try {
                    if (stepJobId == null) {
                        st = client.checkAppState(appJobId);
                        System.out.println("App state: " + st.getJobState());
                        if (st.getJobState().equals("suspend"))
                            throw new IllegalStateException();
                        stepJobId = st.getStepJobIds().get("step2");
                    }
                    if (stepJobId != null) {
                        ret = client.checkJob(stepJobId);
                        System.out.println("Job finished: " + ret.getFinished());
                        if (ret.getFinished() != null && ret.getFinished() == 1L) {
                            break;
                        }
                        List<LogLine> lines = client.getJobLogs(new GetJobLogsParams().withJobId(stepJobId)
                                .withSkipLines((long)logLinesRecieved)).getLines();
                        int blockCount = 0;
                        for (LogLine line : lines) {
                            System.out.println("LOG: " + line.getLine());
                            if (line.getLine().startsWith("["))
                                blockCount++;
                        }
                        if (blockCount == 1)
                            numberOfOneLiners++;
                        logLinesRecieved += lines.size();
                    }
                    Thread.sleep(2500);
                } catch (ServerException ex) {
                    System.out.println(ex.getData());
                    throw ex;
                }
            }
            Assert.assertNotNull(ret);
            String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(ret);
            Assert.assertEquals(errMsg, 1L, (long)ret.getFinished());
            Assert.assertNotNull(errMsg, ret.getResult());
            List<Object> data = ret.getResult().asClassInstance(List.class);
            Assert.assertEquals(errMsg, 1, data.size());
            Assert.assertEquals(errMsg, 3, data.get(0));
            Assert.assertEquals(errMsg, 3, numberOfOneLiners);
            Assert.assertEquals(2, execStats.size());
            LogExecStatsParams execLog = execStats.get(1);
            Assert.assertEquals(moduleName, execLog.getAppModuleName());
            Assert.assertEquals(methodName, execLog.getAppId());
            Assert.assertEquals(moduleName, execLog.getFuncModuleName());
            Assert.assertEquals(methodName, execLog.getFuncName());
            Assert.assertEquals(serviceVer, execLog.getGitCommitHash());
            double queueTime = execLog.getExecStartTime() - execLog.getCreationTime();
            double execTime = execLog.getFinishTime() - execLog.getExecStartTime();
            Assert.assertTrue("" + execLog, queueTime > 0);
            Assert.assertTrue("" + execLog, execTime > 15);
            Assert.assertTrue("" + execLog, execTime < 120);
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }

    @Test
    public void testError() throws Exception {
        System.out.println("Test [testError]");
        try {
            AppState st = runAsyncMethodAsAppAndWait("onerepotest", "generate_error", "\"Super!\"");
            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
            String stepErrorText = st.getStepErrors().get("step1");
            Assert.assertNotNull(errMsg, stepErrorText);
            Assert.assertTrue(st.toString(), stepErrorText.contains("ValueError: Super!"));
            Assert.assertTrue(st.toString(), stepErrorText.contains("Preparing to generate an error..."));
            Assert.assertEquals(errMsg, RunAppBuilder.APP_STATE_ERROR, st.getJobState());
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }

    @Test
    public void testConfig() throws Exception {
        System.out.println("Test [testConfig]");
        try {
            AppState st = runAsyncMethodAsAppAndWait("onerepotest", "get_deploy_config");
            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
            Assert.assertEquals(errMsg, "completed", st.getJobState());
            Assert.assertNotNull(errMsg, st.getStepOutputs());
            String step1output = st.getStepOutputs().get("step1");
            Assert.assertNotNull(errMsg, step1output);
            List<Map<String, String>> data = UObject.getMapper().readValue(step1output, List.class);
            Assert.assertEquals(errMsg, 1, data.size());
            Map<String, String> output = data.get(0);
            Assert.assertNotNull(errMsg, output);
            Assert.assertNotNull(errMsg, output.get("kbase-endpoint"));
            Assert.assertTrue(errMsg, output.get("kbase-endpoint").startsWith("http"));
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }

    @Test
    public void testPythonWrongType() throws Exception {
        System.out.println("Test [testPythonWrongType]");
        try {
            AppState st = runAsyncMethodAsAppAndWait("onerepotest", "print_lines", "123");
            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
            Assert.assertEquals(errMsg, "suspend", st.getJobState());
            Assert.assertNotNull(errMsg, st.getStepErrors());
            String step1err = st.getStepErrors().get("step1");
            Assert.assertNotNull(errMsg, step1err);
            Assert.assertTrue(step1err, step1err.contains("positional arg #1 is the wrong type"));
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }

    @Test
    public void testRefData() throws Exception {
        System.out.println("Test [testRefData]");
        String refDataFileName = "test.txt";
        PrintWriter pw = new PrintWriter(new File(refDataDir, refDataFileName));
        pw.println("Reference data file");
        pw.close();
        try {
            AppState st = runAsyncMethodAsAppAndWait("onerepotest", "list_ref_data", "\"/data\"");
            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
            Assert.assertEquals(errMsg, "completed", st.getJobState());
            Assert.assertNotNull(errMsg, st.getStepOutputs());
            String step1output = st.getStepOutputs().get("step1");
            Assert.assertNotNull(errMsg, step1output);
            List<List<String>> data = UObject.getMapper().readValue(step1output, List.class);
            Assert.assertTrue(errMsg, new TreeSet<String>(data.get(0)).contains(refDataFileName));
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }
    
    @Test
    public void testAsyncClient() throws Exception {
        System.out.println("Test [testAsyncClient]");
        String refDataFileName = "test.txt";
        PrintWriter pw = new PrintWriter(new File(refDataDir, refDataFileName));
        pw.println("Reference data file");
        pw.close();
        OnerepotestClient cl = new OnerepotestClient(client.getURL(), client.getToken());
        cl.setIsInsecureHttpConnectionAllowed(true);
        List<String> ret = cl.listRefData("/data");
        Assert.assertTrue(new TreeSet<String>(ret).contains(refDataFileName));
    }
    
    @Test
    public void testWrongMethod() throws Exception {
        System.out.println("Test [testWrongMethod]");
        try {
            AppState st = runAsyncMethodAsAppAndWait("onerepotest", "filter_contigs");
            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
            Assert.assertEquals(errMsg, "suspend", st.getJobState());
            Assert.assertNotNull(errMsg, st.getStepErrors().get("step1"));
            Assert.assertTrue(errMsg, st.getStepErrors().get("step1").contains("Error: Unknown error"));
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }

    @Test
    public void testLocalSdkCallback() throws Exception {
        System.out.println("Test [testLocalSdkCallback]");
        try {
            String inputText = "123\n456";
            AppState st = runAsyncMethodAsAppAndWait("onerepotest", "local_sdk_callback", 
                    UObject.getMapper().writeValueAsString(inputText));
            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
            Assert.assertEquals(errMsg, "completed", st.getJobState());
            Assert.assertNotNull(errMsg, st.getStepOutputs());
            String step1output = st.getStepOutputs().get("step1");
            Assert.assertNotNull(errMsg, step1output);
            List<String> data = UObject.getMapper().readValue(step1output, List.class);
            Assert.assertEquals(errMsg, inputText, data.get(0));
            Assert.assertEquals(errMsg, "OK", data.get(1));
        } catch (ServerException ex) {
            System.err.println(ex.getData());
            throw ex;
        }
    }

    public String lookupServiceVersion(String moduleName) throws Exception,
            IOException, InvalidFileFormatException, JsonClientException {
        CatalogClient cat = getCatalogClient(token, loadConfig());
        String ver = cat.getModuleInfo(new SelectOneModuleParams().withModuleName(moduleName)).getDev().getGitCommitHash();
        return ver;
    }

    private static WorkspaceClient getWsClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        String wsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
        WorkspaceClient ret = new WorkspaceClient(new URL(wsUrl), auth);
        ret.setAuthAllowedForHttp(true);
        return ret;
    }

    private static CatalogClient getCatalogClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        String catUrl = config.get(NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        CatalogClient ret = new CatalogClient(new URL(catUrl), auth);
        ret.setAllSSLCertificatesTrusted(true);
        ret.setIsInsecureHttpConnectionAllowed(true);
        return ret;
    }

    private static String token(Properties props) throws Exception {
        return AuthService.login(get(props, "user"), get(props, "password")).getTokenString();
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
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties props = props(new File("test.cfg"));
        AuthToken token = new AuthToken(token(props));
        AweClientDockerJobScriptTest.token = token;
        workDir = prepareWorkDir("awe-integration");
        File scriptFile = new File(workDir, "check_deps.sh");
        writeFileLines(readReaderLines(new InputStreamReader(
                AweClientDockerJobScriptTest.class.getResourceAsStream(
                        "check_deps.sh.properties"))), scriptFile);
        ProcessHelper.cmd("bash", scriptFile.getCanonicalPath()).exec(workDir);
        mongoDir = new File(workDir, "mongo");
        aweServerDir = new File(workDir, "awe_server");
        aweClientDir = new File(workDir, "awe_client");
        njsServiceDir = new File(workDir, "njs_service");
        File binDir = new File(njsServiceDir, "bin");
        int mongoPort = startupMongo(null, mongoDir);
        File aweBinDir = new File(workDir, "deps/bin").getCanonicalFile();
        int awePort = startupAweServer(findAweBinary(aweBinDir, "awe-server"), aweServerDir, mongoPort);
        catalogWrapper = startupCatalogWrapper();
        njsService = startupNJSService(njsServiceDir, binDir, awePort, 
                catalogWrapper.getConnectors()[0].getLocalPort(), mongoPort);
        int jobServicePort = njsService.getConnectors()[0].getLocalPort();
        startupAweClient(findAweBinary(aweBinDir, "awe-client"), aweClientDir, awePort, binDir);
        client = new NarrativeJobServiceClient(new URL("http://localhost:" + jobServicePort), token);
        client.setIsInsecureHttpConnectionAllowed(true);
        String machineName = java.net.InetAddress.getLocalHost().getHostName();
        machineName = machineName == null ? "nowhere" : machineName.toLowerCase().replaceAll("[^\\dA-Za-z_]|\\s", "_");
        long suf = System.currentTimeMillis();
        WorkspaceClient wscl = getWsClient(token, loadConfig());
        Exception error = null;
        for (int i = 0; i < 5; i++) {
            testWsName = "test_feature_values_" + machineName + "_" + suf;
            try {
                wscl.createWorkspace(new CreateWorkspaceParams().withWorkspace(testWsName));
                error = null;
                break;
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                error = ex;
            }
        }
        if (error != null)
            throw error;
        File dir = new File("test_data");
        GZIPInputStream is = new GZIPInputStream(new FileInputStream(new File(dir, "Rhodobacter.contigset.json.gz")));
        Map<String, Object> contigsetData = UObject.getMapper().readValue(is, Map.class);
        is.close();
        wscl.saveObjects(new SaveObjectsParams().withWorkspace(testWsName).withObjects(Arrays.asList(
                new ObjectSaveData().withName(testContigsetObjName).withType("KBaseGenomes.ContigSet")
                .withData(new UObject(contigsetData)))));
        /*is = new GZIPInputStream(new FileInputStream(new File(dir, "Rhodobacter.genome.json.gz")));
        Map<String, Object> genomeData = UObject.getMapper().readValue(is, Map.class);
        is.close();
        String genomeObjName = "temp_contigset.1";
        genomeData.put("contigset_ref", testWsName + "/" + testContigsetObjName);
        wscl.saveObjects(new SaveObjectsParams().withWorkspace(testWsName).withObjects(Arrays.asList(
                new ObjectSaveData().withName(genomeObjName).withType("KBaseGenomes.Genome")
                .withData(new UObject(genomeData)))));*/
        refDataDir = new File(njsServiceDir, "onerepotest/0.2");
        if (!refDataDir.exists())
            refDataDir.mkdirs();
    }
    
    private static String findAweBinary(File dir, String program) throws Exception {
        if (new File(dir, program).exists())
            return new File(dir, program).getAbsolutePath();
        return program;
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        if (njsService != null) {
            try {
                njsService.stop();
                System.out.println(njsServiceDir.getName() + " was stopped");
            } catch (Exception ignore) {}
        }
        killPid(aweClientDir);
        killPid(aweServerDir);
        //killPid(shockDir);
        killPid(mongoDir);
        try {
            if (testWsName != null) {
                //getWsClient().deleteWorkspace(new WorkspaceIdentity().withWorkspace(testWsName));
                //System.out.println("Test workspace " + testWsName + " was deleted");
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static int startupMongo(String mongodExePath, File dir) throws Exception {
        if (mongodExePath == null)
            mongodExePath = "mongod";
        if (!dir.exists())
            dir.mkdirs();
        File dataDir = new File(dir, "data");
        dataDir.mkdir();
        File logFile = new File(dir, "mongodb.log");
        int port = findFreePort();
        File configFile = new File(dir, "mongod.conf");
        writeFileLines(Arrays.asList(
                "dbpath=" + dataDir.getAbsolutePath(),
                "logpath=" + logFile.getAbsolutePath(),
                "logappend=true",
                "port=" + port,
                "bind_ip=127.0.0.1"
                ), configFile);
        File scriptFile = new File(dir, "start_mongo.sh");
        writeFileLines(Arrays.asList(
                "#!/bin/bash",
                "cd " + dir.getAbsolutePath(),
                mongodExePath + " --config " + configFile.getAbsolutePath() + " >out.txt 2>err.txt & pid=$!",
                "echo $pid > pid.txt"
                ), scriptFile);
        ProcessHelper.cmd("bash", scriptFile.getCanonicalPath()).exec(dir);
        boolean ready = false;
        for (int n = 0; n < mongoInitWaitSeconds; n++) {
            Thread.sleep(1000);
            if (logFile.exists()) {
                if (grep(readFileLines(logFile), "waiting for connections on port " + port).size() > 0) {
                    ready = true;
                    break;
                }
            }
        }
        if (!ready) {
            if (logFile.exists())
                for (String l : readFileLines(logFile))
                    System.err.println("MongoDB log: " + l);
            throw new IllegalStateException("MongoDB couldn't startup in " + mongoInitWaitSeconds + " seconds");
        }
        System.out.println(dir.getName() + " was started up");
        return port;
    }

    private static int startupAweServer(String aweServerExePath, File dir, int mongoPort) throws Exception {
        if (aweServerExePath == null) {
            aweServerExePath = "awe-server";
        }
        if (!dir.exists())
            dir.mkdirs();
        File dataDir = new File(dir, "data");
        dataDir.mkdir();
        File logsDir = new File(dir, "logs");
        logsDir.mkdir();
        File siteDir = new File(dir, "site");
        siteDir.mkdir();
        File awfDir = new File(dir, "awfs");
        awfDir.mkdir();
        int port = findFreePort();
        File configFile = new File(dir, "awe.cfg");
        writeFileLines(Arrays.asList(
                "[Admin]",
                "email=shock-admin@kbase.us",
                "users=" + get(props(new File("test.cfg")), "user"),
                "[Anonymous]",
                "read=true",
                "write=true",
                "delete=true",
                "cg_read=false",
                "cg_write=false",
                "cg_delete=false",
                "[Args]",
                "debuglevel=0",
                "[Auth]",
                "globus_token_url=https://nexus.api.globusonline.org/goauth/token?grant_type=client_credentials",
                "globus_profile_url=https://nexus.api.globusonline.org/users",
                "client_auth_required=false",
                "[Directories]",
                "data=" + dataDir.getAbsolutePath(),
                "logs=" + logsDir.getAbsolutePath(),
                "site=" + siteDir.getAbsolutePath(),
                "awf=" + awfDir.getAbsolutePath(),
                "[Mongodb]",
                "hosts=localhost:" + mongoPort,
                "database=AWEDB",
                "[Mongodb-Node-Indices]",
                "id=unique:true",
                "[Ports]",
                "site-port=" + findFreePort(),
                "api-port=" + port
                ), configFile);
        File scriptFile = new File(dir, "start_awe_server.sh");
        writeFileLines(Arrays.asList(
                "#!/bin/bash",
                "cd " + dir.getAbsolutePath(),
                aweServerExePath + " --conf " + configFile.getAbsolutePath() + " >out.txt 2>err.txt & pid=$!",
                "echo $pid > pid.txt"
                ), scriptFile);
        ProcessHelper.cmd("bash", scriptFile.getCanonicalPath()).exec(dir);
        Exception err = null;
        for (int n = 0; n < aweServerInitWaitSeconds; n++) {
            Thread.sleep(1000);
            try {
                InputStream is = new URL("http://localhost:" + port + "/job/").openStream();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> ret = mapper.readValue(is, Map.class);
                if (ret.containsKey("limit") && ret.containsKey("total_count")) {
                    err = null;
                    break;
                } else {
                    err = new Exception("AWE server response doesn't match expected data: " + 
                            mapper.writeValueAsString(ret));
                }
            } catch (Exception ex) {
                err = ex;
            }
        }
        if (err != null) {
            File errorFile = new File(new File(logsDir, "server"), "error.log");
            if (errorFile.exists())
                for (String l : readFileLines(errorFile))
                    System.err.println("AWE server error: " + l);
            throw new IllegalStateException("AWE server couldn't startup in " + aweServerInitWaitSeconds + 
                    " seconds (" + err.getMessage() + ")", err);
        }
        System.out.println(dir.getName() + " was started up");
        return port;
    }

    private static int startupAweClient(String aweClientExePath, File dir, int aweServerPort, 
            File binDir) throws Exception {
        if (aweClientExePath == null) {
            aweClientExePath = "awe-client";
        }
        if (!dir.exists())
            dir.mkdirs();
        File dataDir = new File(dir, "data");
        dataDir.mkdir();
        File logsDir = new File(dir, "logs");
        logsDir.mkdir();
        File workDir = new File(dir, "work");
        workDir.mkdir();
        int port = findFreePort();
        File configFile = new File(dir, "awec.cfg");
        writeFileLines(Arrays.asList(
                "[Directories]",
                "data=" + dataDir.getAbsolutePath(),
                "logs=" + logsDir.getAbsolutePath(),
                "[Args]",
                "debuglevel=0",
                "[Client]",
                "workpath=" + workDir.getAbsolutePath(),
                "supported_apps=" + NarrativeJobServiceServer.AWE_CLIENT_SCRIPT_NAME,
                "serverurl=http://localhost:" + aweServerPort + "/",
                "group=kbase",
                "name=kbase-client",
                "auto_clean_dir=false",
                "worker_overlap=false",
                "print_app_msg=true",
                "clientgroup_token=",
                "pre_work_script=",
                "pre_work_script_args="
                ), configFile);
        File scriptFile = new File(dir, "start_awe_client.sh");
        writeFileLines(Arrays.asList(
                "#!/bin/bash",
                "cd " + dir.getAbsolutePath(),
                "export PATH=" + binDir.getAbsolutePath() + ":$PATH",
                aweClientExePath + " --conf " + configFile.getAbsolutePath() + " >out.txt 2>err.txt & pid=$!",
                "echo $pid > pid.txt"
                ), scriptFile);
        ProcessHelper.cmd("bash", scriptFile.getCanonicalPath()).exec(dir);
        Exception err = null;
        for (int n = 0; n < aweClientInitWaitSeconds; n++) {
            Thread.sleep(1000);
            try {
                InputStream is = new URL("http://localhost:" + aweServerPort + "/client/").openStream();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> ret = mapper.readValue(is, Map.class);
                if (ret.containsKey("data") && 
                        ((List<Object>)ret.get("data")).size() > 0) {
                    err = null;
                    break;
                } else {
                    err = new Exception("AWE client response doesn't match expected data: " + 
                            mapper.writeValueAsString(ret));
                }
            } catch (Exception ex) {
                err = ex;
            }
        }
        if (err != null) {
            File errorFile = new File(new File(logsDir, "client"), "error.log");
            if (errorFile.exists())
                for (String l : readFileLines(errorFile))
                    System.err.println("AWE client error: " + l);
            throw new IllegalStateException("AWE client couldn't startup in " + aweClientInitWaitSeconds + 
                    " seconds (" + err.getMessage() + ")", err);
        }
        System.out.println(dir.getName() + " was started up");
        return port;
    }

    private static Server startupNJSService(File dir, File binDir, int awePort, 
            int catalogPort, int mongoPort) throws Exception {
        if (!dir.exists())
            dir.mkdirs();
        if (!binDir.exists())
            binDir.mkdirs();
        int exitCode = ProcessHelper.cmd("ant", "script", "-Djardir=" + dir.getAbsolutePath(), 
                "-Dbindir=" + binDir.getAbsolutePath()).exec(new File(".")).getProcess().exitValue();
        if (exitCode != 0)
            throw new IllegalStateException("Error compiling command line script");
        initSilentJettyLogger();
        File configFile = new File(dir, "deploy.cfg");
        int port = findFreePort();
        Map<String, String> origConfig = loadConfig();
        Properties testProps = props(new File("test.cfg"));
        List<String> configLines = new ArrayList<String>(Arrays.asList(
                "[" + NarrativeJobServiceServer.SERVICE_DEPLOYMENT_NAME + "]",
                NarrativeJobServiceServer.CFG_PROP_SCRATCH + "=" + dir.getAbsolutePath(),
                NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL),
                NarrativeJobServiceServer.CFG_PROP_SHOCK_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL),
                //NarrativeJobServiceServer.CFG_PROP_QUEUE_DB_DIR + "=" + new File(dir, "queue").getAbsolutePath(),
                NarrativeJobServiceServer.CFG_PROP_AWE_SRV_URL + "=http://localhost:" + awePort + "/",
                NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL),
                NarrativeJobServiceServer.CFG_PROP_RUNNING_TASKS_PER_USER + "=5",
                NarrativeJobServiceServer.CFG_PROP_THREAD_COUNT + "=2",
                NarrativeJobServiceServer.CFG_PROP_REBOOT_MODE + "=false",
                NarrativeJobServiceServer.CFG_PROP_ADMIN_USER_NAME + "=kbasetest,rsutormin",
                NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL),
                NarrativeJobServiceServer.CFG_PROP_NJS_SRV_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_NJS_SRV_URL),
                NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL + "=http://localhost:" + catalogPort,
                NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT),
                NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL + "=http://localhost:" + port + "/",
                NarrativeJobServiceServer.CFG_PROP_REF_DATA_BASE + "=" + dir.getCanonicalPath(),
                NarrativeJobServiceServer.CFG_PROP_CATALOG_ADMIN_USER + "=" + get(testProps, "user"),
                NarrativeJobServiceServer.CFG_PROP_CATALOG_ADMIN_PWD + "=" + get(testProps, "password"),
                NarrativeJobServiceServer.CFG_PROP_DEFAULT_AWE_CLIENT_GROUPS + "=kbase",
                NarrativeJobServiceServer.CFG_PROP_NARRATIVE_PROXY_SHARING_USER + "=rsutormin",
                NarrativeJobServiceServer.CFG_PROP_AWE_READONLY_ADMIN_USER + "=" + get(testProps, "user"),
                NarrativeJobServiceServer.CFG_PROP_AWE_READONLY_ADMIN_PWD + "=" + get(testProps, "password"),
                NarrativeJobServiceServer.CFG_PROP_MONGO_HOSTS + "=localhost:" + mongoPort,
                NarrativeJobServiceServer.CFG_PROP_MONGO_DBNAME + "=exec_engine"
                ));
        String dockerURI = origConfig.get(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI);
        if (dockerURI != null)
            configLines.add(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI + "=" + dockerURI);
        writeFileLines(configLines, configFile);
        System.setProperty("KB_DEPLOYMENT_CONFIG", configFile.getAbsolutePath());
        Server jettyServer = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        jettyServer.setHandler(context);
        context.addServlet(new ServletHolder(new NarrativeJobServiceServer()),"/*");
        jettyServer.start();
        Exception err = null;
        JsonClientCaller caller = new JsonClientCaller(new URL("http://localhost:" + port + "/"));
        for (int n = 0; n < njsJobWaitSeconds; n++) {
            Thread.sleep(1000);
            try {
                caller.jsonrpcCall("Unknown", new ArrayList<String>(), null, false, false);
            } catch (ServerException ex) {
                if (ex.getMessage().contains("Can not find method [NarrativeJobService.Unknown] in server class")) {
                    err = null;
                    break;
                } else {
                    err = ex;
                }
            } catch (Exception ex) {
                err = ex;
            }
        }
        if (err != null)
            throw new IllegalStateException("NarrativeJobService couldn't startup in " + 
                    njsJobWaitSeconds + " seconds (" + err.getMessage() + ")", err);
        System.out.println(dir.getName() + " was started up");
        return jettyServer;
    }

    private static Server startupCatalogWrapper() throws Exception {
        initSilentJettyLogger();
        JsonServerServlet catalogSrv = new CatalogWrapper();
        int port = findFreePort();
        Server jettyServer = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        jettyServer.setHandler(context);
        context.addServlet(new ServletHolder(catalogSrv),"/*");
        jettyServer.start();
        Exception err = null;
        JsonClientCaller caller = new JsonClientCaller(new URL("http://localhost:" + port + "/"));
        for (int n = 0; n < njsJobWaitSeconds; n++) {
            Thread.sleep(1000);
            try {
                caller.jsonrpcCall("Unknown", new ArrayList<String>(), null, false, false);
            } catch (ServerException ex) {
                if (ex.getMessage().contains("Can not find method [Catalog.Unknown] in server class")) {
                    err = null;
                    break;
                } else {
                    err = ex;
                }
            } catch (Exception ex) {
                err = ex;
            }
        }
        if (err != null)
            throw new IllegalStateException("Catalog wrapper couldn't startup in " + 
                    njsJobWaitSeconds + " seconds (" + err.getMessage() + ")", err);
        System.out.println("Catalog wrapper was started up");
        return jettyServer;
    }

    public static void initSilentJettyLogger() {
        Log.setLog(new Logger() {
            @Override
            public void warn(String arg0, Object arg1, Object arg2) {}
            @Override
            public void warn(String arg0, Throwable arg1) {}
            @Override
            public void warn(String arg0) {}
            @Override
            public void setDebugEnabled(boolean arg0) {}
            @Override
            public boolean isDebugEnabled() {
                return false;
            }
            @Override
            public void info(String arg0, Object arg1, Object arg2) {}
            @Override
            public void info(String arg0) {}
            @Override
            public String getName() {
                return null;
            }
            @Override
            public Logger getLogger(String arg0) {
                return this;
            }
            @Override
            public void debug(String arg0, Object arg1, Object arg2) {}
            @Override
            public void debug(String arg0, Throwable arg1) {}
            @Override
            public void debug(String arg0) {}
        });
    }
    
    public static Map<String, String> loadConfig() throws IOException,
            InvalidFileFormatException {
        Ini ini = new Ini(new File("deploy.cfg"));
        Map<String, String> origConfig = ini.get(NarrativeJobServiceServer.SERVICE_DEPLOYMENT_NAME);
        return origConfig;
    }

    private static void killPid(File dir) {
        if (dir == null)
            return;
        try {
            File pidFile = new File(dir, "pid.txt");
            if (pidFile.exists()) {
                String pid = readFileLines(pidFile).get(0).trim();
                ProcessHelper.cmd("kill", pid).exec(dir);
                System.out.println(dir.getName() + " was stopped");
            }
        } catch (Exception ignore) {}
    }
    
    private static void writeFileLines(List<String> lines, File targetFile) throws IOException {
        PrintWriter pw = new PrintWriter(targetFile);
        for (String l : lines)
            pw.println(l);
        pw.close();
    }

    private static List<String> readFileLines(File f) throws IOException {
        return readReaderLines(new FileReader(f));
    }
    
    private static List<String> readReaderLines(Reader r) throws IOException {
        List<String> ret = new ArrayList<String>();
        BufferedReader br = new BufferedReader(r);
        while (true) {
            String l = br.readLine();
            if (l == null)
                break;
            ret.add(l);
        }
        br.close();
        return ret;
    }

    private static List<String> grep(List<String> lines, String substring) {
        List<String> ret = new ArrayList<String>();
        for (String l : lines)
            if (l.contains(substring))
                ret.add(l);
        return ret;
    }
    
    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {}
        throw new IllegalStateException("Can not find available port in system");
    }
    
    public static File prepareWorkDir(String testName) throws IOException {
        File tempDir = new File("temp_files").getCanonicalFile();
        if (!tempDir.exists())
            tempDir.mkdirs();
        for (File dir : tempDir.listFiles()) {
            if (dir.isDirectory() && dir.getName().startsWith("test_" + testName + "_"))
                try {
                    deleteRecursively(dir);
                } catch (Exception e) {
                    System.out.println("Can not delete directory [" + dir.getName() + "]: " + e.getMessage());
                }
        }
        File workDir = new File(tempDir, "test_" + testName + "_" + System.currentTimeMillis());
        if (!workDir.exists())
            workDir.mkdir();
        return workDir;
    }
    
    private static void deleteRecursively(File fileOrDir) {
        if (fileOrDir.isDirectory() && !Files.isSymbolicLink(fileOrDir.toPath()))
            for (File f : fileOrDir.listFiles()) 
                deleteRecursively(f);
        fileOrDir.delete();
    }
    
    public static void main(String[] args) throws Exception {
        beforeClass();
        int port = njsService.getConnectors()[0].getLocalPort();
        System.out.println("NarrativeJobService was started up on port: " + port);
    }
    
    public static class CatalogWrapper extends JsonServerServlet {
        private static final long serialVersionUID = 1L;
        
        public CatalogWrapper() {
            super("Catalog");
        }
        
        private CatalogClient fwd() throws IOException, JsonClientException {
            Map<String, String> origConfig = loadConfig();
            String url = origConfig.get(NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
            CatalogClient ret = new CatalogClient(new URL(url));
            ret.setAllSSLCertificatesTrusted(true);
            ret.setIsInsecureHttpConnectionAllowed(true);
            return ret;
        }
        
        @JsonServerMethod(rpc = "Catalog.get_module_info")
        public ModuleInfo getModuleInfo(SelectOneModuleParams selection) throws IOException, JsonClientException {
            return fwd().getModuleInfo(selection);
        }
        
        @JsonServerMethod(rpc = "Catalog.get_version_info")
        public ModuleVersionInfo getVersionInfo(SelectModuleVersionParams params) throws IOException, JsonClientException {
            return fwd().getVersionInfo(params);
        }
        
        @JsonServerMethod(rpc = "Catalog.log_exec_stats")
        public void logExecStats(LogExecStatsParams params, AuthToken authPart) throws IOException, JsonClientException {
            execStats.add(params);
        }

        @JsonServerMethod(rpc = "Catalog.get_client_groups")
        public List<AppClientGroup> getClientGroups(GetClientGroupParams params) throws IOException, JsonClientException {
            return Arrays.asList(new AppClientGroup().withClientGroups(Arrays.asList("*")));
        }
    }
}
