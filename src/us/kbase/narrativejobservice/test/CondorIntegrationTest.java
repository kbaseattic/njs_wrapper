package us.kbase.narrativejobservice.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import junit.framework.Assert;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.ini4j.InvalidFileFormatException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.junit.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;


import us.kbase.auth.AuthToken;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ClientGroupConfig;
import us.kbase.catalog.ClientGroupFilter;
import us.kbase.catalog.GetSecureConfigParamsInput;
import us.kbase.catalog.LogExecStatsParams;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.ModuleVersion;
import us.kbase.catalog.ModuleVersionInfo;
import us.kbase.catalog.SecureConfigParameter;
import us.kbase.catalog.SelectModuleVersion;
import us.kbase.catalog.SelectModuleVersionParams;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.catalog.VolumeMount;
import us.kbase.catalog.VolumeMountConfig;
import us.kbase.catalog.VolumeMountFilter;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple11;
import us.kbase.common.service.Tuple13;
import us.kbase.common.service.Tuple2;
import us.kbase.common.service.Tuple3;
import us.kbase.common.service.UObject;
import us.kbase.common.test.TestException;
import us.kbase.common.test.controllers.mongo.MongoController;
import us.kbase.common.utils.AweUtils;
import us.kbase.common.utils.ProcessHelper;
import us.kbase.narrativejobservice.CancelJobParams;
import us.kbase.narrativejobservice.CheckJobCanceledResult;
import us.kbase.narrativejobservice.CheckJobsParams;
import us.kbase.narrativejobservice.CheckJobsResults;
import us.kbase.narrativejobservice.GetJobLogsParams;
import us.kbase.narrativejobservice.JobState;
import us.kbase.narrativejobservice.JsonRpcError;
import us.kbase.narrativejobservice.LogLine;
import us.kbase.narrativejobservice.NarrativeJobServiceClient;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.narrativejobservice.RunJobParams;
import us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.workspace.CreateWorkspaceParams;
import us.kbase.workspace.GetObjects2Params;
import us.kbase.workspace.ObjectData;
import us.kbase.workspace.ObjectSaveData;
import us.kbase.workspace.ObjectSpecification;
import us.kbase.workspace.ProvenanceAction;
import us.kbase.workspace.SaveObjectsParams;
import us.kbase.workspace.SubAction;
import us.kbase.workspace.WorkspaceClient;
import us.kbase.workspace.WorkspaceIdentity;

import us.kbase.narrativejobservice.test.CatalogRegForTests;
import us.kbase.workspace.RegisterTypespecParams;
import us.kbase.workspace.ListModuleVersionsParams;
import us.kbase.workspace.GetModuleInfoParams;
import us.kbase.workspace.GrantModuleOwnershipParams;

import com.fasterxml.jackson.core.JsonParseException;
import us.kbase.common.service.JsonTokenStream;

import org.apache.commons.lang3.ArrayUtils;



@SuppressWarnings("unchecked")
public class CondorIntegrationTest {
    private static AuthToken token = null;
    private static NarrativeJobServiceClient client = null;
    private static File workDir = null;
    private static File mongoDir = null;
    //private static File shockDir = null;
    private static File aweServerDir = null;
    private static int awePort = -1;
    private static File aweClientDir = null;
    private static File njsServiceDir = null;
    private static Server catalogWrapper = null;
    private static Server njsService = null;
    private static String testWsName = null;
    private static long testWsID = 0;
    private static final String testContigsetObjName = "temp_contigset.1";
    private static File refDataDir = null;

    private static int aweServerInitWaitSeconds = 60;
    private static int aweClientInitWaitSeconds = 60;
    private static int njsJobWaitSeconds = 60;
    private static MongoController mongo;

    private static String STAGED1_NAME = "staged1";
    private static String STAGED2_NAME = "staged2";
    private static Map<String, String> NAME2REF =
            new HashMap<String, String>();

    private static List<LogExecStatsParams> execStats = Collections.synchronizedList(
            new ArrayList<LogExecStatsParams>());

    private final static DateTimeFormatter DATE_PARSER =
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    .appendOptional(DateTimeFormat.forPattern(".SSS").getParser())
                    .append(DateTimeFormat.forPattern("Z"))
                    .toFormatter();






    public static void requestOwnership(WorkspaceClient wc, String moduleName) throws  Exception{
        try {
            wc.requestModuleOwnership(moduleName);
            System.out.println("REQUESTING OWNERSHIP OF " + moduleName );
        }
        catch(ServerException e){
            System.out.println(e);
        }
        try {
            String approveModeRequestCommand = "{" + "\"command\": \"approveModRequest\" " + ',' + "\"module\":" + '"' + moduleName + '"' + "}";
            System.out.println(approveModeRequestCommand);
            UObject response = wc.administer(UObject.fromJsonString(approveModeRequestCommand));
        }
        catch (ServerException e){
            System.out.println(e);
        }

    }


    public static void registerModule(String moduleName,  String specfilePath, List typesList) throws Exception{
        WorkspaceClient wc = getWsClient(token, TesterUtils.loadConfig());
        String username = token.getUserName();
        requestOwnership(wc, moduleName);

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(new File(specfilePath)));
        while (true) {
            String l = br.readLine();
            if (l == null)
                break;
            sb.append(l).append("\n");
        }
        br.close();
        System.out.println(wc.registerTypespec(new RegisterTypespecParams()
                .withSpec(sb.toString()).withNewTypes(typesList).withDryrun(0L)));
        System.out.println(wc.listModuleVersions(new ListModuleVersionsParams().withMod(moduleName)));
        System.out.println(wc.getModuleInfo(new GetModuleInfoParams().withMod(moduleName)));
        wc.releaseModule(moduleName);
    }


    public static void registerTypes() throws Exception{
        //Check registration for list of modules

        try{
            List typesList =  Arrays.asList("ContigSet", "Genome", "GenomeComparison","Pangenome");
            registerModule("KBaseGenomes","./test_data/specfiles/KBaseGenomes.spec", typesList);
        }catch (Exception e){
            System.out.println(e);
        }

        try{
            List typesList = Arrays.asList("AType", "AHandle", "ARef");
            registerModule("Empty","./test_data/specfiles/Empty.spec", typesList);
        }catch (Exception e){
            System.out.println(e);
        }
    }


//    @Test
//    public void testCheckJob() throws Exception{
//        Properties props = TesterUtils.props();
//        String njs_url = props.getProperty("njs_server_url");
//        System.out.println("Test [testOneJob]");
//        System.out.println("Connecting to server:" + njs_url);
//        System.out.println("Using Token:" + props.getProperty("token"));
//        client = new NarrativeJobServiceClient(new URL(njs_url), token);
//        client.setIsInsecureHttpConnectionAllowed(true);
//
//        String jobId = "5ad18e1ce4b00b63d04e188d";
//        JobState ret = null;
//        JobState ret2 = null;
//        for (int i = 0; i < 5; i++) {
//            System.out.println("ATTEMPTING TO CHECK JOB" + jobId);
//            try {
//                System.out.println("ATTEMPTING TO CHECK JOB" + jobId);
//                ret = client.checkJobs(new CheckJobsParams().withJobIds(
//                        Arrays.asList(jobId)).withWithJobParams(1L)).getJobStates().get(jobId);
//
////                if(ret == null){
////                    throw new IllegalStateException("(Are you root?) Error: couldn't check job:" + jobId);
////                }
//                if (ret.getFinished() != null && ret.getFinished() == 1L) {
//                    System.out.println("Job finished: " + ret.getFinished());
//                    break;
//                }
//                System.out.println("STATUS = ");
//                Thread.sleep(2000);
//            } catch (ServerException ex) {
//                System.out.println(ex.getData());
//                throw ex;
//            }
//        }
//    }


    public static String runApp(String moduleName, String methodName, String serviceVer, String jsonInput) throws Exception{
        execStats.clear();
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("foo", "bar");
        RunJobParams params = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer)
                .withAppId("myapp/foo").withMeta(meta).withWsid(testWsID)
                .withParams(Arrays.asList(UObject.fromJsonString(jsonInput)));
        return client.runJob(params);
    }


    public static JobState checkJobStatusCompletein60(String jobId) throws Exception{
        JobState ret = null;
        for (int i = 0; i < 30; i++) {
            try {
                ret = client.checkJobs(new CheckJobsParams().withJobIds(
                        Arrays.asList(jobId)).withWithJobParams(1L)).getJobStates().get(jobId);
                if (ret!= null && ret.getFinished() != null && ret.getFinished() == 1L) {
                    System.out.println("Job finished: " + ret.getFinished());
                    return ret;
                }
                System.out.println("Checking status for:" + jobId);
                Thread.sleep(2000);
            } catch (ServerException ex) {
                System.out.println(ex.getData());
                throw ex;
            }
        }
        if(ret == null){
            throw new IllegalStateException("(Are you root?) Error: couldn't check job:" + jobId);
        }
        return null;
    }


//    @Test
//    public void testSubmitSimpleJobToCompletion() throws Exception{
//        Properties props = TesterUtils.props();
//        String njs_url = props.getProperty("njs_server_url");
//        client = new NarrativeJobServiceClient(new URL(njs_url), token);
//        client.setIsInsecureHttpConnectionAllowed(true);
//
//        String moduleName = "simpleapp";
//        String methodName = "simple_add";
//
//        String jsonInput = "{\"base_number\":\"101\"}";
//        String serviceVer = lookupServiceVersion(moduleName);
//
//        String jobID = runApp(moduleName,methodName,serviceVer,jsonInput);
//        assertNotNull(jobID);
//        System.out.println("Submitted job and got" + jobID);
//        JobState ret = checkJobStatusCompletein60(jobID);
//        System.out.println("JOB STATE =");
//        System.out.println(ret);
//        assertNotNull(ret);
//
//        List<Map<String, Map<String, String>>> data = ret.getResult().asClassInstance(List.class);
//        Map<String, String> outParams = data.get(0).get("result");
//        Assert.assertEquals(outParams.get("new_number"), 101 + 100);
//
//    }

    @Ignore @Test
    public void testDeleteJob() throws Exception{
        System.out.println("Test [testDeleteJob]");
        String moduleName = "simpleapp";
        String methodName = "simple_add";
        String serviceVer = lookupServiceVersion(moduleName);
        String jobID = runApp(moduleName,methodName,serviceVer,"{\"base_number\":\"101\"}");
        System.out.println("ABOUT TO CANCEL: " + jobID);
        Thread.sleep(10000);
        client.cancelJob(new CancelJobParams().withJobId(jobID));
    }


    @Ignore @Test
    public void testSimpleJobWithParent() throws Exception{
        Properties props = TesterUtils.props();
        String njs_url = props.getProperty("njs_server_url");
        System.out.println("Test [testSimpleJobWithParent]");
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("foo", "bar");

        //ParentJob
        String moduleName = "simpleapp";
        String methodName = "simple_add";
        String serviceVer = lookupServiceVersion(moduleName);
        RunJobParams params = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer)
                .withAppId("myapp/foo").withMeta(meta).withWsid(testWsID)
                .withParams(Arrays.asList(UObject.fromJsonString("{\"base_number\":\"101\"}")));
        String jobId = client.runJob(params);
        assertNotNull(jobId);

        //ChildJob
        RunJobParams params2 = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer)
                .withAppId("myapp/foo").withMeta(meta).withWsid(testWsID)
                .withParams(Arrays.asList(UObject.fromJsonString("{\"base_number\":\"101\"}"))).withParentJobId(jobId);
        String jobId_child1 = client.runJob(params2);
        assertNotNull(jobId_child1);

        //ChildJob
        RunJobParams params3 = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer)
                .withAppId("myapp/foo").withMeta(meta).withWsid(testWsID)
                .withParams(Arrays.asList(UObject.fromJsonString("{\"base_number\":\"101\"}"))).withParentJobId(jobId);
        String jobId_child2 = client.runJob(params3);
        assertNotNull(jobId_child2);

       JobState ret = client.checkJob(jobId);
        List<String> child_jobs = new ArrayList<String>();
        child_jobs.add(jobId_child1);
        child_jobs.add(jobId_child2);

        List<String> subjobs = (List<String>)ret.getAdditionalProperties().get("sub_jobs");
        System.out.println("Asserting child jobs match sub jobs");
        System.out.println(subjobs);
        System.out.println(child_jobs);
        assertTrue(child_jobs.containsAll(subjobs) && subjobs.containsAll(child_jobs));

        System.out.println("Cancelling jobs");
        client.cancelJob(new CancelJobParams().withJobId(jobId));
        client.cancelJob(new CancelJobParams().withJobId(jobId_child1));
        client.cancelJob(new CancelJobParams().withJobId(jobId_child2));
    }


    @Ignore @Test
    public void testSimpleJobWithCancelJob() throws Exception {
        Properties props = TesterUtils.props();
        String njs_url = props.getProperty("njs_server_url");
        System.out.println("Test [testSimpleJob + get job status]");
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("foo", "bar");

        execStats.clear();
        String basenumber = "101";
        String moduleName = "simpleapp";
        //simple_add_with_sleep cuases a sleep job to be launched
        String methodName = "simple_add_with_sleep";
        String serviceVer = lookupServiceVersion(moduleName);
        RunJobParams params = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer)
                .withAppId("myapp/foo").withMeta(meta).withWsid(testWsID)
                .withParams(Arrays.asList(UObject.fromJsonString("{\"base_number\":\"101\"}")));
        String jobId = client.runJob(params);
        assertNotNull(jobId);
        System.out.println("Submitted job and got: " + jobId);
        JobState ret = null;

        System.out.println("Sleeping");

        //Sleep until the job starts up a 120 second sleep thread

        String id_path = "/mnt/condor/wsadmin/" + jobId + "/docker_job_ids/";

        File f = new File(id_path);
        File[] list = f.listFiles();


        for(int i = 0; i < 20; i++){
            Thread.sleep(4000);
            System.out.println("Examining:" + id_path);
            list = f.listFiles();
            System.out.println("Docker job ids are:");
            if(list != null) {
                for (File item : list) {
                    System.out.println(item);
                }
            }
        }

//        System.out.println("Cancelling job");
//        client.cancelJob(new CancelJobParams().withJobId(jobId));
//
//        System.out.println("Checking to see if jobs have exited");
//
//        System.out.println(list);



    }



    @Ignore @Test
    public void testSimpleJob() throws Exception {
        Properties props = TesterUtils.props();
        String njs_url = props.getProperty("njs_server_url");
        System.out.println("Test [testSimpleJob + get job status]");
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("foo", "bar");

        execStats.clear();
        String basenumber = "101";
        String moduleName = "simpleapp";
        String methodName = "simple_add";
        String serviceVer = lookupServiceVersion(moduleName);
        RunJobParams params = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer)
                .withAppId("myapp/foo").withMeta(meta).withWsid(testWsID)
                .withParams(Arrays.asList(UObject.fromJsonString("{\"base_number\":\"101\"}")));
        String jobId = client.runJob(params);
        assertNotNull(jobId);
        System.out.println("Submitted job and got: " + jobId);
        JobState ret = null;



        long FinishState = 0;
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(2000);
                ret = client.checkJob(jobId);
                if(ret==null){
                    System.out.println(String.format("jobid%s is not yet finished", jobId));
                    continue;
                }
                else if (ret.getFinished() != null && ret.getFinished() == 1L) {
                    break;
                }
                else{
                    System.out.println(ret);
                }

            } catch (ServerException ex) {
                System.out.println(ex.getData());
                throw ex;
            }
        }
        if(ret == null){
            throw new IllegalStateException("(Are you root?) Error: couldn't check job:" + jobId);
        }
        if (ret.getFinished() != null && ret.getFinished() == 1L) {
            System.out.println("Job finished: " + ret.getFinished());
            System.out.println(ret);
        }
        assertTrue(ret.getFinished() == 1L);

        System.out.println("Asserting that the result is:" + basenumber);
        UObject new_number = ret.getResult().asList().get(0).asMap().get("new_number");
        assertTrue(new_number.toJsonString().contains("" + (Integer.parseInt(basenumber) + 100)));

    }


//    @Test
//    public void testOneJob() throws Exception {
//
//        Properties props = TesterUtils.props();
//        String njs_url = props.getProperty("njs_server_url");
//        System.out.println("Connecting to server:" + njs_url);
//        System.out.println("Using Token:" + props.getProperty("token"));
//
//
//        client = new NarrativeJobServiceClient(new URL(njs_url), token);
//        client.setIsInsecureHttpConnectionAllowed(true);
//
//
//
//        System.out.println("Test [testOneJob]");
//        Map<String, String> meta = new HashMap<String, String>();
//        meta.put("foo", "bar");
//        try {
//            execStats.clear();
//            String moduleName = "simple2";
//            String methodName = "simple_add";
//            String serviceVer = lookupServiceVersion(moduleName);
//            RunJobParams params = new RunJobParams().withMethod(
//                    moduleName + "." + methodName).withServiceVer(serviceVer)
//                    .withAppId("myapp/foo").withMeta(meta).withWsid(testWsID)
//                    .withParams(Arrays.asList(UObject.fromJsonString("{\"new_number\":\"101\"}")));
//
//            String jobId = client.runJob(params);
//            JobState ret = null;
//            for (int i = 0; i < 20; i++) {
//                try {
//                    CheckJobsResults retAll = client.checkJobs(new CheckJobsParams().withJobIds(
//                            Arrays.asList(jobId)).withWithJobParams(1L));
//                    ret = retAll.getJobStates().get(jobId);
//                    if (ret == null) {
//                        JsonRpcError error = retAll.getCheckError().get(jobId);
//                        System.out.println("Error: " + error);
//                        throw new IllegalStateException("Error: " + error);
//                    }
//                    System.out.println("Job finished: " + ret.getFinished());
//                    if (ret.getFinished() != null && ret.getFinished() == 1L) {
//                        break;
//                    }
//                    Thread.sleep(5000);
//                } catch (ServerException ex) {
//                    System.out.println(ex.getData());
//                    throw ex;
//                }
//            }
//            Assert.assertNotNull(ret);
//            String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(ret);
//            Assert.assertEquals(errMsg, 1L, (long)ret.getFinished());
//            Assert.assertNotNull(errMsg, ret.getResult());
//            assertThat("incorrect appid", params.getAppId(), is("myapp/foo"));
//            List<Map<String, Map<String, String>>> data = ret.getResult().asClassInstance(List.class);
//            Assert.assertEquals(errMsg, 1, data.size());
//            Map<String, String> outParams = data.get(0).get("params");
//            Assert.assertNotNull(errMsg, outParams);
//            Assert.assertEquals(errMsg, "myws.mygenome1", outParams.get("genomeA"));
//            Assert.assertEquals(errMsg, "myws.mygenome2", outParams.get("genomeB"));
//            Assert.assertEquals(1, execStats.size());
//            LogExecStatsParams execLog = execStats.get(0);
//            Assert.assertEquals("myapp", execLog.getAppModuleName());
//            Assert.assertEquals("foo", execLog.getAppId());
//            Assert.assertEquals(moduleName, execLog.getFuncModuleName());
//            Assert.assertEquals(methodName, execLog.getFuncName());
//            Assert.assertEquals(serviceVer, execLog.getGitCommitHash());
//            double queueTime = execLog.getExecStartTime() - execLog.getCreationTime();
//            double execTime = execLog.getFinishTime() - execLog.getExecStartTime();
//            Assert.assertTrue("" + execLog, queueTime > 0);
//            Assert.assertTrue("" + execLog, execTime > 0);
//
//            //check input params
//            params = client.getJobParams(jobId).getE1();
//            assertThat("incorrect appid", params.getAppId(), is("myapp/foo"));
//
//            //check UJS job
//            Tuple13<String, Tuple2<String, String>, String, String, String,
//                    Tuple3<String, String, String>, Tuple3<Long, Long, String>,
//                    Long, Long, Tuple2<String, String>, Map<String, String>,
//                    String, Results> u = getUJSClient(token, TesterUtils.loadConfig())
//                    .getJobInfo2(jobId);
//            assertThat("incorrect metadata", u.getE11(), is(meta));
//            assertThat("incorrect auth strat", u.getE10().getE1(),
//                    is("kbaseworkspace"));
//            assertThat("incorrect ws id", u.getE10().getE2(),
////                    is("" + testWsID));
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }

    private Map<String, Object> buildInsanitaryObject() {
        Map<String, Object> inner = new HashMap<String, Object>();
        inner.put("$$.%%%bad...$$%%%key", "value");
        inner.put("key1", 1);
        inner.put("key2", null);
        inner.put("key3", true);
        Map<String, Object> outer = new HashMap<String, Object>();
        outer.put("id", "foo");
        outer.put("bad%.$key$.%", "value");
        outer.put("key", Arrays.asList(inner));
        outer.put("key2", 2);
        outer.put("key4", null);
        outer.put("key5", false);
        return outer;
    }

//    @Test
//    public void testInsanitaryParams() throws Exception {
//        System.out.println("Test [testInsanitaryParams]");
//        Map<String, Object> outer = buildInsanitaryObject();
//
//        JobState js = runJob("njs_sdk_test_3.run", "dev", new UObject(outer),
//                null);
//        Tuple2<RunJobParams, Map<String, String>> rjp =
//                client.getJobParams(js.getJobId());
//        Map<String, Object> got = rjp.getE1().getParams().get(0)
//                .asClassInstance(Map.class);
//        assertThat("incorrect params", got, is(outer));
//    }
//
//    @Test
//    public void testInsanitaryReturns() throws Exception {
//        System.out.println("Test [testInsanitaryReturns]");
//        Map<String, Object> ret = buildInsanitaryObject();
//        String ref = saveObjectToWs(ret, "testInsanitaryReturns");
//        Map<String, Object> params = new HashMap<String, Object>();
//        params.put("id", "bar");
//        params.put("ret", ref);
//
//        JobState js = runJob("njs_sdk_test_3.run", "dev", new UObject(params),
//                null);
//        System.out.println(js.getResult());
//        List<Map<String, Object>> got =
//                js.getResult().asClassInstance(List.class);
//        assertThat("incorrect result",
//                (Map<String, Object>) got.get(0).get("ret"), is(ret));
//
//        JobState jres = client.checkJob(js.getJobId());
//        got = jres.getResult().asClassInstance(List.class);
//        assertThat("incorrect result",
//                (Map<String, Object>) got.get(0).get("ret"), is(ret));
//    }

    private Map<String, Object> buildLargeObject() {

        Map<String, Object> ret = new HashMap<String, Object>();
        for (int i = 0; i < 55765; i++) {
            ret.put("key" + i, "01234");
        }
        ret.put("id", "foo");
        return ret;
    }

//    @Test
//    public void testLargeParams() throws Exception {
//        //note the SDKLocalMethodRunner limits returns to 1m
//        System.out.println("Test [testLargeParams]");
//        Map<String, Object> p = buildLargeObject();
//
//        //should work
//        runJob("njs_sdk_test_3.run", "dev", new UObject(p), null);
//        p.put("foo", "wheeeee");
//
//        try {
//            runJob("njs_sdk_test_3.run", "dev", new UObject(p), null);
//            fail("started job with too large object");
//        } catch (ServerException se) {
//            assertThat("incorrect exception message", se.getLocalizedMessage(),
//                    is("Input parameters are above 1048576B maximum: 1048579"));
//        }
//    }

    private String saveObjectToWs(
            Map<String, Object> ret, final String objname) throws IOException,
            JsonClientException, Exception, InvalidFileFormatException {
        Tuple11<Long, String, String, String, Long, String, Long, String,
                String, Long, Map<String, String>> obj =
                getWsClient(token, TesterUtils.loadConfig())
                        .saveObjects(new SaveObjectsParams()
                                .withWorkspace(testWsName)
                                .withObjects(Arrays.asList(
                                        new ObjectSaveData()
                                                .withData(new UObject(ret))
                                                .withName(objname)
                                                .withType("Empty.AType")
                                        )
                                )).get(0);
        return obj.getE7() + "/" + obj.getE1();
    }

    public static ModuleVersionInfo getMVI(ModuleInfo mi, String release) {
        if (release.equals("dev")) {
            return mi.getDev();
        } else if (release.equals("beta")) {
            return mi.getBeta();
        } else {
            return mi.getRelease();
        }
    }

//    @Test
//    public void testNestedAsync() throws Exception {
//        System.out.println("Test [testNestedAsync]");
//        execStats.clear();
//        String moduleName = "njs_sdk_test_1";
//        String methodName = "run";
//        String objectName = "async-basic";
//        String release = "dev";
//        String ver = "0.0.3";
//        final String modmeth = moduleName + "." + methodName;
//        Map<String, Object> p = ImmutableMap.<String, Object>builder()
//                .put("save", ImmutableMap.<String,Object>builder()
//                        .put("ws", testWsName)
//                        .put("name", objectName)
//                        .build()
//                )
//                .put("jobs", Arrays.asList(
//                        ImmutableMap.<String, Object>builder()
//                                .put("method", modmeth)
//                                .put("params", Arrays.asList(
//                                        ImmutableMap.<String, Object>builder()
//                                                .put("wait", 10)
//                                                .put("id", "inner1").build()))
//                                .put("ver", release)
//                                .put("cli_async", true)
//                                .build(),
//                        ImmutableMap.<String, Object>builder()
//                                .put("method", modmeth)
//                                .put("params", Arrays.asList(
//                                        ImmutableMap.<String, Object>builder()
//                                                .put("wait", 5)
//                                                .put("id", "inner2")
//                                                .put("jobs", Arrays.asList(
//                                                        ImmutableMap.<String, Object>builder()
//                                                                .put("method", modmeth)
//                                                                .put("params", Arrays.asList(
//                                                                        ImmutableMap.<String, Object>builder()
//                                                                                .put("wait", 3)
//                                                                                .put("id", "inner2_1").build()))
//                                                                .put("ver", release)
//                                                                .put("cli_async", true)
//                                                                .build()
//                                                )).build()
//                                ))
//                                .put("ver", release)
//                                .put("cli_async", true)
//                                .build()
//                        )
//                )
//                .put("id", "outer")
//                .put("run_jobs_async", true)
//                .build();
//        List<SubActionSpec> expsas = new LinkedList<SubActionSpec>();
//        expsas.add(new SubActionSpec()
//                .withMod(moduleName)
//                .withVer("0.0.3")
//                .withRel("dev")
//        );
//        JobState res = runJobAndCheckProvenance(moduleName, methodName,
//                release, ver, new UObject(p), objectName, expsas,
//                Arrays.asList(STAGED1_NAME));
//        System.out.println("Results:\n" + res.getResult()
//                .asClassInstance(List.class));
//        checkResults(res, p, moduleName);
//        checkLoggingComplete(res);
//    }

    private void checkLoggingComplete(JobState res) throws Exception {
        List<LogLine> lines = client.getJobLogs(new GetJobLogsParams()
                .withJobId(res.getJobId())
                .withSkipLines(0L)).getLines();
        Assert.assertTrue(!lines.isEmpty());
        Assert.assertEquals("Job is done", lines.get(lines.size() - 1)
                .getLine());
        // Let's check that AWE script is done
        String aweServerUrl = "http://localhost:" + awePort;
        String aweJobId = (String)res.getAdditionalProperties()
                .get("awe_job_id");
        String aweState = null;
        for (int i = 0; i < 5; i++) {
            Map<String, Object> aweJob = AweUtils.getAweJobDescr(
                    aweServerUrl, aweJobId, token);
            Map<String, Object> aweData =
                    (Map<String, Object>)aweJob.get("data");
            if (aweData != null)
                aweState = (String)aweData.get("state");
            if (aweState != null && aweState.equals(SDKMethodRunner.APP_STATE_DONE))
                break;
            Thread.sleep(1000);
        }
        Assert.assertNotNull(aweState);
        Assert.assertEquals(SDKMethodRunner.APP_STATE_DONE, aweState);
    }

    private void checkResults(JobState res, Map<String, Object> params,
                              String name) {
        Map<String, Object> got = (Map<String, Object>) res.getResult()
                .asClassInstance(List.class).get(0);
        checkResults(got, params, name);
    }

    private void checkResults(Map<String, Object> got,
                              Map<String, Object> params, String name) {
        assertThat("incorrect name", (String) got.get("name"), is(name));
        if (params.containsKey("wait")) {
            assertThat("incorrect wait time", (Integer) got.get("wait"),
                    is(params.get("wait")));
        }
        assertThat("incorrect id", (String) got.get("id"),
                is(params.get("id")));
        System.out.println(params.get("id"));
        assertNotNull("missing hash", (String) got.get("hash"));
        List<Map<String, Object>> parjobs =
                (List<Map<String, Object>>) params.get("jobs");
        if (params.containsKey("jobs")) {
            List<List<Map<String,Object>>> gotjobs =
                    (List<List<Map<String, Object>>>) got.get("jobs");
            assertNotNull("missing jobs", gotjobs);
            assertThat("not same number of jobs", gotjobs.size(),
                    is(parjobs.size()));
            Iterator<List<Map<String, Object>>> gotiter = gotjobs.iterator();
            Iterator<Map<String, Object>> pariter = parjobs.iterator();
            while (gotiter.hasNext()) {
                Map<String, Object> p = pariter.next();
                String modmeth = (String) p.get("method");
                String module = modmeth.split("\\.")[0];
                //results are always wrapped in a list
                checkResults(gotiter.next().get(0),
                        //as are parameters
                        ((List<Map<String, Object>>) p.get("params")).get(0),
                        (String) module);
            }
        }
    }

//    @Test
//    public void testBasicProvenance() throws Exception {
//        System.out.println("Test [testBasicProvenance]");
//        execStats.clear();
//        String moduleName = "njs_sdk_test_2";
//        String methodName = "run";
//        String objectName = "prov-basic";
//        String release = "dev";
//        String ver = "0.0.9";
//        UObject methparams = UObject.fromJsonString(
//                "{\"save\": {\"ws\":\"" + testWsName + "\"," +
//                        "\"name\":\"" + objectName + "\"" +
//                        "}," +
//                        "\"id\": \"myid\"" +
//                        "}");
//        List<SubActionSpec> expsas = new LinkedList<SubActionSpec>();
//        expsas.add(new SubActionSpec()
//                .withMod(moduleName)
//                .withVer(ver)
//                .withRel(release)
//        );
//        JobState res = runJobAndCheckProvenance(moduleName, methodName,
//                release, ver, methparams, objectName, expsas,
//                Arrays.asList(STAGED1_NAME));
//        checkResults(res, methparams.asClassInstance(Map.class), moduleName);
//        checkLoggingComplete(res);
//
//        release = "beta";
//        ver = "0.0.8";
//        expsas.set(0, new SubActionSpec()
//                .withMod(moduleName)
//                .withVer(ver)
//                .withRel(release)
//        );
//        res = runJobAndCheckProvenance(moduleName, methodName, release, ver,
//                methparams, objectName, expsas, Arrays.asList(STAGED1_NAME));
//        checkResults(res, methparams.asClassInstance(Map.class), moduleName);
//        checkLoggingComplete(res);
//    }
//
//    @Test
//    public void testMultiCallProvenance() throws Exception {
//        // TODO: run
//        System.out.println("Test [testMultiCallProvenance]");
//        execStats.clear();
//        String moduleName = "njs_sdk_test_1";
//        String moduleName2 = "njs_sdk_test_2";
//        String methodName = "run";
//        String objectName = "prov_multi";
//        String release = "dev";
//        String ver = "0.0.3";
//        String repo1commit = "de445aa9c3404d68be3a87b03c1dbf2f3fccba24";
//        String repo2commit = "3cd0ed213d8376349bdb0f454c5f5bc8b31ea650";
//        UObject methparams = UObject.fromJsonString(String.format(
//                "{\"save\": {\"ws\":\"%s\"," +
//                        "\"name\":\"%s\"" +
//                        "}," +
//                        "\"jobs\": [{\"method\": \"%s\"," +
//                        "\"params\": [{\"id\": \"id1\", \"wait\": 3}]," +
//                        "\"ver\": \"%s\"" +
//                        "}," +
//                        "{\"method\": \"%s\"," +
//                        "\"params\": [{\"id\": \"id2\", \"wait\": 3}]," +
//                        "\"ver\": \"%s\"" +
//                        "}," +
//                        "{\"method\": \"%s\"," +
//                        "\"params\": [{\"id\": \"id3\", \"wait\": 3}]," +
//                        "\"ver\": \"%s\"" +
//                        "}" +
//                        "]," +
//                        "\"id\": \"myid\"" +
//                        "}", testWsName, objectName,
//                moduleName2 + "." + methodName,
//                // dev is on this commit
//                repo2commit,
//                moduleName + "." + methodName,
//                // this is the latest commit, but a prior commit is registered
//                //for dev
//                repo1commit,
//                moduleName2 + "." + methodName,
//                "dev"));
//        List<SubActionSpec> expsas = new LinkedList<SubActionSpec>();
//        expsas.add(new SubActionSpec()
//                .withMod(moduleName)
//                .withVer("0.0.3")
//                .withRel("dev")
//        );
//        expsas.add(new SubActionSpec()
//                .withMod(moduleName2)
//                .withVer("0.0.8")
//                .withCommit(repo2commit)
//        );
//        JobState res = runJobAndCheckProvenance(moduleName, methodName,
//                release, ver, methparams, objectName, expsas,
//                Arrays.asList(STAGED2_NAME, STAGED1_NAME));
//        checkResults(res, methparams.asClassInstance(Map.class), moduleName);
//        checkLoggingComplete(res);
//    }
//
//    @Test
//    public void testBadWSIDs() throws Exception {
//        System.out.println("Test [testBadWSIDs]");
//        List<String> ws = new ArrayList<String>(Arrays.asList(
//                testWsName + "/" + "objectdoesntexist",
//                testWsName + "/" + STAGED1_NAME,
//                "fakeWSName/foo"));
//        List<String> input = new LinkedList<String>(ws);
//        ws.remove(1);
//        failJobWSRefs(input, String.format("The workspace objects %s either" +
//                        " don't exist or were inaccessible to the user %s.",
//                ws, token.getUserName()));
//    }
//
//    @Test
//    public void testWorkspaceError() throws Exception {
//        System.out.println("Test [testWorkspaceError]");
//        // test a workspace error.
//        List<String> input = new ArrayList<String>(Arrays.asList(
//                testWsName + "/" + STAGED1_NAME,
//                testWsName + "/" + "objectdoesntexist/badver"));
//        failJobWSRefs(input, String.format("Error on ObjectSpecification #2:" +
//                " Unable to parse version portion of object reference " +
//                testWsName + "/objectdoesntexist/badver to an integer"));
//    }
//
//    @Test
//    public void testBadRelease() throws Exception {
//        System.out.println("Test [testBadRelease]");
//        // note that dev and beta releases can only have one version each,
//        // version tracking only happens for prod
//
//        failJob("njs_sdk_test_1foo.run", "beta",
//                "Error looking up module njs_sdk_test_1foo with version " +
//                        "beta: Module cannot be found based on module_name or " +
//                        "git_url parameters.");
//        failJob("njs_sdk_test_1.run", "beta",
//                "Error looking up module njs_sdk_test_1 with version " +
//                        "beta: No module version found that matches your criteria!");
//        failJob("njs_sdk_test_1.run", "release",
//                "Error looking up module njs_sdk_test_1 with version " +
//                        "release: No module version found that matches your criteria!");
//        failJob("njs_sdk_test_1.run", null,
//                "Error looking up module njs_sdk_test_1 with version " +
//                        "release: No module version found that matches your criteria!");
//
//        //this is the newest git commit and was registered in dev but
//        //then the previous git commit was registered in dev
//        String git = "b0d487271c22f793b381da29e266faa9bb0b2d1b";
//        failJob("njs_sdk_test_1.run", git,
//                "Error looking up module njs_sdk_test_1 with version " +
//                        git + ": No module version found that matches your criteria!");
//        failJob("njs_sdk_test_1.run", "foo",
//                "Error looking up module njs_sdk_test_1 with version foo: " +
//                        "No module version found that matches your criteria!");
//    }
//
//    @Test
//    public void testfailJobMultiCallBadRelease() throws Exception {
//        System.out.println("Test [testfailJobMultiCallBadRelease]");
//
//        failJobMultiCall(
//                "njs_sdk_test_2.run", "njs_sdk_test_1foo.run", "dev", "null",
//                "Error looking up module njs_sdk_test_1foo with version " +
//                        "release: Module cannot be found based on module_name or " +
//                        "git_url parameters.");
//        failJobMultiCall(
//                "njs_sdk_test_2.run", "njs_sdk_test_1.run", "dev", "\"beta\"",
//                "Error looking up module njs_sdk_test_1 with version " +
//                        "beta: No module version found that matches your criteria!");
//        failJobMultiCall(
//                "njs_sdk_test_2.run", "njs_sdk_test_1.run", "dev", "\"release\"",
//                "Error looking up module njs_sdk_test_1 with version " +
//                        "release: No module version found that matches your criteria!");
//        failJobMultiCall(
//                "njs_sdk_test_2.run", "njs_sdk_test_1.run", "dev", "null",
//                "Error looking up module njs_sdk_test_1 with version " +
//                        "release: No module version found that matches your criteria!");
//        //this is the newest git commit and was registered in dev but
//        //then the previous git commit was registered in dev
//        String git = "b0d487271c22f793b381da29e266faa9bb0b2d1b";
//        failJobMultiCall(
//                "njs_sdk_test_2.run", "njs_sdk_test_1.run", "dev",
//                "\"b0d487271c22f793b381da29e266faa9bb0b2d1b\"",
//                "Error looking up module njs_sdk_test_1 with version " +
//                        git + ": No module version found that matches your criteria!");
//        failJobMultiCall(
//                "njs_sdk_test_2.run", "njs_sdk_test_1.run", "dev", "\"foo\"",
//                "Error looking up module njs_sdk_test_1 with version foo: " +
//                        "No module version found that matches your criteria!");
//    }
//
//    @Test
//    public void testfailJobBadMethod() throws Exception {
//        System.out.println("Test [testfailJobBadMethod]");
//        failJob("njs_sdk_test_1run", "foo",
//                "Illegal method name: njs_sdk_test_1run");
//        failJob("njs_sdk_test_1.r.un", "foo",
//                "Illegal method name: njs_sdk_test_1.r.un");
//    }
//
//    @Test
//    public void testfailJobMultiCallBadMethod() throws Exception {
//        System.out.println("Test [testfailJobMultiCallBadMethod]");
//        failJobMultiCall(
//                "njs_sdk_test_2.run", "njs_sdk_test_1run", "dev", "null",
//                "Can not find method [CallbackServer.njs_sdk_test_1run] in " +
//                        "server class us.kbase.narrativejobservice.subjobs." +
//                        "NJSCallbackServer");
//        failJobMultiCall(
//                "njs_sdk_test_2.run", "njs_sdk_test_1.r.un", "dev", "null",
//                "Illegal method name: njs_sdk_test_1.r.un");
//
//    }
//
//    @Test
//    public void testErrorInSubjob() throws Exception {
//        System.out.println("Test [testErrorInSubjob]");
//        execStats.clear();
//        String moduleName = "njs_sdk_test_1";
//        String methodName = "run";
//        String methparams = String.format(
//                "{\"save\": {\"ws\":\"%s\"," +
//                        "\"name\":\"%s\"" +
//                        "}," +
//                        "\"jobs\": [{\"method\": \"onerepotest.generate_error\"," +
//                        "\"params\": [\"Custom error message!\"]," +
//                        "\"ver\": \"dev\"" +
//                        "}" +
//                        "]," +
//                        "\"id\": \"myid\"" +
//                        "}", testWsName, "unused_object");
//        JobState st = runAsyncMethodAndWait(moduleName, methodName, methparams);
//        List<LogLine> lines = client.getJobLogs(new GetJobLogsParams().withJobId(st.getJobId())
//                .withSkipLines(0L)).getLines();
//        String textForSearch =
//                "\"onerepotest.generate_error\" job threw an error, name=\"Server error\", code=-32000, " +
//                        "message=\"Custom error message!\", data:";
//        boolean found = false;
//        for (LogLine l : lines) {
//            if (l.getIsError() == 1 && l.getLine().contains(textForSearch)) {
//                found = true;
//                break;
//            }
//        }
//        Assert.assertTrue(found);
//    }

    private void failJobMultiCall(String outerModMeth, String innerModMeth,
                                  String outerRel, String innerRel, String msg) throws Exception {
        UObject methparams = UObject.fromJsonString(String.format(
                "{\"jobs\": [{\"method\": \"%s\"," +
                        "\"params\": [{\"id\": \"id1\"}]," +
                        "\"ver\": %s" +
                        "}" +
                        "]," +
                        "\"id\": \"myid\"" +
                        "}",
                innerModMeth, innerRel));
        JobState ret = runJob(outerModMeth, outerRel, methparams,
                null);
        assertThat("correct error message", ret.getError().getMessage(),
                is(msg));
    }

    private void failJobWSRefs(List<String> refs, String exp) throws Exception{
        UObject mt = new UObject(new HashMap<String, String>());
        try {
            runJob("foo", "bar", "baz", mt, refs);
            fail("Ran bad job");
        } catch (ServerException se) {
            assertThat("correct exception", se.getLocalizedMessage(), is(exp));
        }
    }

    private void failJob(String moduleMeth, String release, String exp)
            throws Exception{
        UObject mt = new UObject(new HashMap<String, String>());
        try {
            runJob(moduleMeth, release, mt, null);
            fail("Ran bad job");
        } catch (ServerException se) {
            assertThat("correct exception", se.getLocalizedMessage(), is(exp));
        }
    }

    public static class SubActionSpec {
        public String module;
        public String release;
        public String ver;
        public String commit;

        public SubActionSpec (){}
        public SubActionSpec withMod(String mod) {
            this.module = mod;
            return this;
        }

        public SubActionSpec withRel(String rel) {
            this.release = rel;
            return this;
        }

        public SubActionSpec withVer(String ver) {
            this.ver = ver;
            return this;
        }

        public SubActionSpec withCommit(String commit) {
            this.commit = commit;
            return this;
        }
        public String getVerRel() {
            if (release == null) {
                return ver;
            }
            return ver + "-" + release;
        }
    }
    private JobState runJobAndCheckProvenance(
            String moduleName,
            String methodName,
            String release,
            String ver,
            UObject methparams,
            String objectName,
            List<SubActionSpec> subs,
            List<String> wsobjs)
            throws IOException, JsonClientException, InterruptedException,
            ServerException, Exception, InvalidFileFormatException {
        List<String> wsobjrefs = new LinkedList<String>();
        for (String o: wsobjs) {
            wsobjrefs.add(testWsName + "/" + o);
        }
        JobState res = runJob(moduleName, methodName, release, methparams,
                wsobjrefs);
        if (res.getError() != null) {
            System.out.println("Job had unexpected error:");
            System.out.println(res.getError());
            throw new TestException(res.getError().getMessage());
        }
        checkProvenance(moduleName, methodName, release, ver, methparams,
                objectName, subs, wsobjs);
        return res;
    }

    private void checkProvenance(
            String moduleName,
            String methodName,
            String release,
            String ver,
            UObject methparams,
            String objectName,
            List<SubActionSpec> subs,
            List<String> wsobjs)
            throws Exception, IOException, InvalidFileFormatException,
            JsonClientException {
        if (release != null) {
            ver = ver + "-" + release;
        }

        WorkspaceClient ws = getWsClient(token, TesterUtils.loadConfig());
        ObjectData od = ws.getObjects2(new GetObjects2Params().withObjects(Arrays.asList(
                new ObjectSpecification().withWorkspace(testWsName)
                        .withName(objectName)))).getData().get(0);
        System.out.println(od);
        List<ProvenanceAction> prov = od.getProvenance();
        assertThat("number of provenance actions",
                prov.size(), is(1));
        ProvenanceAction pa = prov.get(0);
        long got = DATE_PARSER.parseDateTime(pa.getTime()).getMillis();
        long now = new Date().getTime();
        assertTrue("got prov time < now ", got < now);
        assertTrue("got prov time > now - 5m", got > now - (5 * 60 * 1000));
        assertThat("correct service", pa.getService(), is(moduleName));
        assertThat("correct service version", pa.getServiceVer(),
                is(ver));
        assertThat("correct method", pa.getMethod(), is(methodName));
        assertThat("number of params", pa.getMethodParams().size(), is(1));
        assertThat("correct params",
                pa.getMethodParams().get(0).asClassInstance(Map.class),
                is(methparams.asClassInstance(Map.class)));
        Set<String> wsobjrefs = new HashSet<String>();
        for (String o: wsobjs) {
            wsobjrefs.add(testWsName + "/" + o);
        }
        assertThat("correct incoming ws objs",
                new HashSet<String>(pa.getInputWsObjects()),
                is(wsobjrefs));
        Iterator<String> reswo = pa.getResolvedWsObjects().iterator();
        for (String wso: pa.getInputWsObjects()) {
            String ref = NAME2REF.get(wso.replace(testWsName + "/", ""));
            assertThat("ref remapped correctly", reswo.next(), is(ref));
        }
        checkSubActions(pa.getSubactions(), subs);
    }

    private void checkSubActions(List<SubAction> gotsas,
                                 List<SubActionSpec> expsas) throws Exception {
        CatalogClient cat = getCatalogClient(token, TesterUtils.loadConfig());
        assertThat("correct # of subactions",
                gotsas.size(), is(expsas.size()));
        for (SubActionSpec sa: expsas) {
            if (sa.commit == null) {
                sa.commit = getMVI(cat.getModuleInfo(
                        new SelectOneModuleParams().withModuleName(sa.module)),
                        sa.release).getGitCommitHash();
            }
        }
        Iterator<SubAction> giter = gotsas.iterator();
        Iterator<SubActionSpec> eiter = expsas.iterator();
        while (giter.hasNext()) {
            SubAction got = giter.next();
            SubActionSpec sa = eiter.next();
            assertThat("correct code url", got.getCodeUrl(),
                    is("https://github.com/kbasetest/" + sa.module));
            assertThat("correct commit", got.getCommit(), is(sa.commit));
            assertThat("correct name", got.getName(), is(sa.module));
            assertThat("correct version", got.getVer(), is(sa.getVerRel()));
        }
    }

    private JobState runJob(String moduleName, String methodName, String release,
                            UObject methparams, List<String> wsobjs)
            throws IOException, JsonClientException,
            InterruptedException, ServerException {
        return runJob(moduleName + "." + methodName, release, methparams,
                wsobjs);
    }

    private JobState runJob(String moduleMethod, String release,
                            UObject methparams, List<String> wsobjs)
            throws IOException, JsonClientException,
            InterruptedException, ServerException {
        RunJobParams params = new RunJobParams()
                .withMethod(moduleMethod)
                .withServiceVer(release)
                .withSourceWsObjects(wsobjs)
                .withParams(Arrays.asList(methparams));
        String jobId = client.runJob(params);
        JobState ret = null;
        for (int i = 0; i < 40; i++) {
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
        if (ret.getResult() == null) {
            System.out.println("Job failed");
            System.out.println(ret);
        }
        return ret;
    }

    private JobState runAsyncMethodAndWait(String moduleName,
                                           String methodName, String... paramsJson) throws Exception,
            IOException, InvalidFileFormatException, JsonClientException,
            InterruptedException, ServerException {
        List<UObject> inputValues = new ArrayList<UObject>();
        for (String paramJson : paramsJson)
            inputValues.add(UObject.fromJsonString(paramJson));
        RunJobParams params = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(lookupServiceVersion(moduleName))
                .withAppId(moduleName + "/" + methodName).withWsid(testWsID)
                .withParams(inputValues);
        String jobId = client.runJob(params);
        JobState ret = null;
        for (int i = 0; i < 60; i++) {
            try {
                ret = client.checkJobs(new CheckJobsParams().withJobIds(
                        Arrays.asList(jobId)).withWithJobParams(1L)).getJobStates().get(jobId);
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
        return ret;
    }

//    @Test
//    public void testLogging() throws Exception {
//        System.out.println("Test [testLogging]");
//        try {
//            execStats.clear();
//            String moduleName = "onerepotest";
//            String methodName = "print_lines";
//            String serviceVer = lookupServiceVersion(moduleName);
//            RunJobParams params = new RunJobParams().withMethod(
//                    moduleName + "." + methodName).withServiceVer(serviceVer)
//                    .withAppId(moduleName + "/" + methodName).withWsid(testWsID)
//                    .withParams(Arrays.asList(UObject.fromJsonString(
//                            "\"First line\\nSecond super long line\\nshort\"")));
//            String jobId = client.runJob(params);
//            JobState ret = null;
//            int logLinesRecieved = 0;
//            int numberOfOneLiners = 0;
//            for (int i = 0; i < 100; i++) {
//                try {
//                    ret = client.checkJob(jobId);
//                    System.out.println("Job finished: " + ret.getFinished());
//                    if (ret.getFinished() != null && ret.getFinished() == 1L) {
//                        break;
//                    }
//                    List<LogLine> lines = client.getJobLogs(new GetJobLogsParams().withJobId(jobId)
//                            .withSkipLines((long)logLinesRecieved)).getLines();
//                    int blockCount = 0;
//                    for (LogLine line : lines) {
//                        System.out.println("LOG: " + line.getLine());
//                        if (line.getLine().startsWith("["))
//                            blockCount++;
//                    }
//                    if (blockCount == 1)
//                        numberOfOneLiners++;
//                    logLinesRecieved += lines.size();
//                    Thread.sleep(1000);
//                } catch (ServerException ex) {
//                    System.out.println(ex.getData());
//                    throw ex;
//                }
//            }
//            Assert.assertNotNull(ret);
//            String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(ret);
//            Assert.assertEquals(errMsg, 1L, (long)ret.getFinished());
//            Assert.assertNotNull(errMsg, ret.getResult());
//            List<Object> data = ret.getResult().asClassInstance(List.class);
//            Assert.assertEquals(errMsg, 1, data.size());
//            Assert.assertEquals(errMsg, 3, data.get(0));
//            Assert.assertEquals(errMsg, 3, numberOfOneLiners);
//            Assert.assertEquals(1, execStats.size());
//            LogExecStatsParams execLog = execStats.get(0);
//            Assert.assertEquals(moduleName, execLog.getAppModuleName());
//            Assert.assertEquals(methodName, execLog.getAppId());
//            Assert.assertEquals(moduleName, execLog.getFuncModuleName());
//            Assert.assertEquals(methodName, execLog.getFuncName());
//            Assert.assertEquals(serviceVer, execLog.getGitCommitHash());
//            double queueTime = execLog.getExecStartTime() - execLog.getCreationTime();
//            double execTime = execLog.getFinishTime() - execLog.getExecStartTime();
//            Assert.assertTrue("" + execLog, queueTime > 0);
//            Assert.assertTrue("" + execLog, execTime > 15);
//            Assert.assertTrue("" + execLog, execTime < 120);
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }
//
//    @Test
//    public void testJobCancellation() throws Exception {
//        // TODO: add tests with several users having and not having read access to UJS job
//        // TODO: add tests with several users for updateJob and finishJob as well
//        System.out.println("Test [testJobCancellation]");
//        try {
//            execStats.clear();
//            String moduleName = "onerepotest";
//            String methodName = "print_lines";
//            String serviceVer = lookupServiceVersion(moduleName);
//            RunJobParams job = new RunJobParams().withMethod(moduleName + "." + methodName)
//                    .withServiceVer(serviceVer)
//                    .withParams(Arrays.asList(new UObject("1\n2\n3\n4\n5\n6\n7\n8\n9")))
//                    .withAppId(moduleName + "/" + methodName);
//            String jobId = client.runJob(job);
//            final CheckJobCanceledResult cres = client.checkJobCanceled(
//                    new CancelJobParams().withJobId(jobId));
//            assertThat("incorrect job id", cres.getJobId(), is(jobId));
//            assertThat("incorrect canceled state", cres.getCanceled(), is(0L));
//            assertThat("incorrect finished state", cres.getFinished(), is(0L));
//            JobState ret = null;
//            int logLinesRecieved = 0;
//            for (int i = 0; i < 100; i++) {
//                try {
//                    ret = client.checkJob(jobId);
//                    System.out.println("Job finished: " + ret.getFinished());
//                    if (ret.getFinished() != null && ret.getFinished() == 1L)
//                        break;
//                    // Executed method (onerepotest.print_lines) prints every line from input text
//                    // with 5 second interval. Lines are printed with square brackets around each.
//                    List<LogLine> lines = client.getJobLogs(new GetJobLogsParams().withJobId(jobId)
//                            .withSkipLines((long)logLinesRecieved)).getLines();
//                    for (LogLine line : lines) {
//                        if (line.getLine().startsWith("[")) {
//                            // We found first line printed by method working in docker container.
//                            // So it's time to cancel the job.
//                            client.cancelJob(new CancelJobParams().withJobId(jobId));
//                        }
//                    }
//                    logLinesRecieved += lines.size();
//                    Thread.sleep(1000);
//                } catch (ServerException ex) {
//                    System.out.println(ex.getData());
//                    throw ex;
//                }
//            }
//            Assert.assertNotNull(ret);
//            String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(ret);
//            Assert.assertEquals(errMsg, 1L, (long)ret.getFinished());
//            Assert.assertEquals(errMsg, 1L, (long)ret.getCanceled());
//            Assert.assertEquals(errMsg, SDKMethodRunner.APP_STATE_CANCELLED, ret.getJobState());
//            Assert.assertEquals(0, execStats.size());
//            final CheckJobCanceledResult cres2 = client.checkJobCanceled(
//                    new CancelJobParams().withJobId(jobId));
//            assertThat("incorrect job id", cres2.getJobId(), is(jobId));
//            assertThat("incorrect canceled state", cres2.getCanceled(), is(1L));
//            assertThat("incorrect finished state", cres2.getFinished(), is(1L));
//            boolean canceledLogLine = false;
//            // Let's check in logs how many lines (out of 9) from input text we see. It depends on
//            // how long it takes to stop docker container really. But we shouldn't see all 9 since
//            // they are printed with 5 second interval.
//            logLinesRecieved = 0;
//            int logLinesFromInput = 0;
//            for (int i = 0; i < 60; i++) {
//                List<LogLine> lines = client.getJobLogs(new GetJobLogsParams().withJobId(jobId)
//                        .withSkipLines((long)logLinesRecieved)).getLines();
//                for (LogLine line : lines) {
//                    String lineText = line.getLine();
//                    if (lineText.startsWith("[") && lineText.endsWith("]"))
//                        logLinesFromInput++;
//                    System.out.println("LOG: " + lineText);
//                    if (line.getLine().contains("Job was canceled")) {
//                        // We see this line in logs only after docker container is stopped
//                        canceledLogLine = true;
//                    }
//                }
//                if (canceledLogLine)
//                    break;
//                logLinesRecieved += lines.size();
//                Thread.sleep(1000);
//            }
//            if (!canceledLogLine) {
//                System.out.println("All logs:");
//                List<LogLine> lines = client.getJobLogs(new GetJobLogsParams().withJobId(jobId)
//                        .withSkipLines((long)0)).getLines();
//                for (LogLine line : lines) {
//                    String lineText = line.getLine();
//                    System.out.println("ALL-LOGs: " + lineText);
//                    if (line.getLine().contains("Job was canceled")) {
//                        canceledLogLine = true;
//                    }
//                }
//                System.out.println("------------------------------------------------");
//            }
//            Assert.assertTrue(errMsg, canceledLogLine);
//            // Since docker stop may take about 10-15 seconds there shouldn't be more than 3-4 log
//            // lines from input. Definitely less than 7.
//            Assert.assertTrue(logLinesFromInput < 7);
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }
//
//    @Test
//    public void testCheckJobCanceledWithBadInput() throws Exception {
//        failCheckJobCanceled(null, "No parameters supplied to method");
//        failCheckJobCanceled(new CancelJobParams().withJobId(null), "No job id supplied");
//        failCheckJobCanceled(new CancelJobParams().withJobId("   \t "), "No job id supplied");
//    }

    private void failCheckJobCanceled(final CancelJobParams p,
                                      final String exception) throws IOException, JsonClientException {
        try {
            client.checkJobCanceled(p);
            fail("ran check job canceled with bad input");
        } catch (ServerException ex) {
            assertThat("incorrect exception message", ex.getMessage(), is(exception));
        }
    }
//
//    @Test
//    public void testError() throws Exception {
//        System.out.println("Test [testError]");
//        try {
//            JobState st = runAsyncMethodAndWait("onerepotest", "generate_error", "\"Super!\"");
//            String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(st);
//            String errorText = checkJobError(st);
//            String expectingError = "ValueError: Super!";
//            Assert.assertTrue(errMsg, errorText.contains(expectingError));
//            List<LogLine> lines = client.getJobLogs(
//                    new GetJobLogsParams().withJobId(st.getJobId())).getLines();
//            String outputLog = "";
//            String errorsLog = "";
//            for (LogLine line : lines) {
//                if (line.getIsError() != null && line.getIsError() == 1L) {
//                    errorsLog += line.getLine() + "\n";
//                } else {
//                    outputLog += line.getLine() + "\n";
//                }
//            }
//            Assert.assertTrue("Output log:\n" + outputLog,
//                    outputLog.contains("Preparing to generate an error..."));
//            Assert.assertTrue("Errors log:\n" + errorsLog, errorsLog.contains(expectingError));
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }

    private String checkJobError(JobState st) throws Exception {
        return checkJobError(st, true);
    }

    private String checkJobError(JobState st, boolean withDetails) throws Exception {
        String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(st);
        Assert.assertEquals(errMsg, SDKMethodRunner.APP_STATE_ERROR, st.getJobState());
        Assert.assertNotNull(errMsg, st.getError());
        String errorText = withDetails ? st.getError().getError() : st.getError().getMessage();
        Assert.assertNotNull(errMsg, errorText);
        return errorText;
    }

    private UObject checkJobOutput(JobState st) throws Exception {
        String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(st);
        Assert.assertEquals(errMsg, SDKMethodRunner.APP_STATE_DONE, st.getJobState());
        Assert.assertNotNull(errMsg, st.getResult());
        UObject ret = st.getResult();
        Assert.assertNotNull(errMsg, ret);
        return ret;
    }
//
//    @Test
//    public void testConfig() throws Exception {
//        System.out.println("Test [testConfig]");
//        try {
//            JobState st = runAsyncMethodAndWait("onerepotest", "get_deploy_config");
//            String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(st);
//            Assert.assertEquals(errMsg, SDKMethodRunner.APP_STATE_DONE, st.getJobState());
//            UObject obj = checkJobOutput(st);
//            List<Map<String, String>> data = obj.asClassInstance(List.class);
//            Assert.assertEquals(errMsg, 1, data.size());
//            Map<String, String> output = data.get(0);
//            Assert.assertNotNull(errMsg, output);
//            Assert.assertNotNull(errMsg, output.get("kbase-endpoint"));
//            Assert.assertTrue(errMsg, output.get("kbase-endpoint").startsWith("http"));
//            Assert.assertEquals(errMsg, "Super password!", output.get("secret-password"));
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }
//
//    @Test
//    public void testPythonWrongType() throws Exception {
//        System.out.println("Test [testPythonWrongType]");
//        try {
//            JobState st = runAsyncMethodAndWait("onerepotest", "print_lines", "123");
//            String errMsg = "Unexpected job state: " + UObject.getMapper().writeValueAsString(st);
//            String errorText = checkJobError(st, false);
//            Assert.assertTrue(errMsg, errorText.contains("positional arg #1 is the wrong type"));
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }
//
//    @Test
//    public void testRefData() throws Exception {
//        System.out.println("Test [testRefData]");
//        String refDataFileName = "test.txt";
//        PrintWriter pw = new PrintWriter(new File(refDataDir, refDataFileName));
//        pw.println("Reference data file");
//        pw.close();
//        try {
//            JobState st = runAsyncMethodAndWait("onerepotest", "list_ref_data", "\"/data\"");
//            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
//            UObject obj = checkJobOutput(st);
//            List<List<String>> data = obj.asClassInstance(List.class);
//            Assert.assertTrue(errMsg, new TreeSet<String>(data.get(0)).contains(refDataFileName));
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }
//
//    @Test
//    public void testCustomData() throws Exception {
//        // CatalogWrapper is configured that it returns non-empty list of volume mappings only if
//        // <work-dir>/<userid> folder exists in host file system. This
//        System.out.println("Test [testCustomData]");
//        String dataFileName = "custom_test.txt";
//        File customDir = new File(workDir, token.getUserName());
//        File customFile = new File(customDir, dataFileName);
//        try {
//            customDir.mkdir();
//            PrintWriter pw = new PrintWriter(customFile);
//            pw.println("Custom data file");
//            pw.close();
//            try {
//                JobState st = runAsyncMethodAndWait("onerepotest", "list_ref_data", "\"/kb/module/custom\"");
//                String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
//                UObject obj = checkJobOutput(st);
//                List<List<String>> data = obj.asClassInstance(List.class);
//                Assert.assertTrue(errMsg, new TreeSet<String>(data.get(0)).contains(dataFileName));
//            } catch (ServerException ex) {
//                System.err.println(ex.getData());
//                throw ex;
//            }
//        } finally {
//            try {
//                if (customFile.exists())
//                    customFile.delete();
//            } catch (Exception ignore) {}
//            try {
//                if (customDir.exists())
//                    customDir.delete();
//            } catch (Exception ignore) {}
//        }
//    }
//
//    @Test
//    public void testAsyncClient() throws Exception {
//        System.out.println("Test [testAsyncClient]");
//        String refDataFileName = "test.txt";
//        PrintWriter pw = new PrintWriter(new File(refDataDir, refDataFileName));
//        pw.println("Reference data file");
//        pw.close();
//        OnerepotestClient cl = new OnerepotestClient(client.getURL(), client.getToken());
//        cl.setIsInsecureHttpConnectionAllowed(true);
//        List<String> ret = cl.listRefData("/data");
//        Assert.assertTrue(new TreeSet<String>(ret).contains(refDataFileName));
//    }
//
//    @Test
//    public void testWrongMethod() throws Exception {
//        System.out.println("Test [testWrongMethod]");
//        try {
//            JobState st = runAsyncMethodAndWait("onerepotest", "filter_contigs");
//            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
//            Assert.assertEquals(errMsg, SDKMethodRunner.APP_STATE_ERROR, st.getJobState());
//            Assert.assertNotNull(errMsg, st.getError());
//            String errorText = st.getError().getName();
//            Assert.assertNotNull(errMsg, errorText);
//            Assert.assertTrue(errMsg, errorText.contains("Method not found"));
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }
//
//    @Test
//    public void testLocalSdkCallback() throws Exception {
//        System.out.println("Test [testLocalSdkCallback]");
//        try {
//            String inputText = "123\n456";
//            JobState st = runAsyncMethodAndWait("onerepotest", "local_sdk_callback",
//                    UObject.getMapper().writeValueAsString(inputText));
//            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(st);
//            UObject obj = checkJobOutput(st);
//            List<String> data = obj.asClassInstance(List.class);
//            Assert.assertEquals(errMsg, inputText, data.get(0));
//            Assert.assertEquals(errMsg, "OK", data.get(1));
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }
//
//    @Test
//    public void testBulkCheckJobs() throws Exception {
//        System.out.println("Test [testBulkCheckJobs]");
//        try {
//            String moduleName = "onerepotest";
//            String methodName = "local_sdk_callback";
//            String inputText = "123\n456";
//            List<UObject> inputValues = Arrays.asList(new UObject(inputText));
//            RunJobParams params = new RunJobParams().withMethod(
//                    moduleName + "." + methodName).withServiceVer(lookupServiceVersion(moduleName))
//                    .withAppId(moduleName + "/" + methodName).withWsid(testWsID)
//                    .withParams(inputValues);
//            String jobId = client.runJob(params);
//            JobState ret = null;
//            String hiddenJobId = "HIDDEN_UNKNOWN_JOB";
//            JsonRpcError hiddenJobError = null;
//            for (int i = 0; i < 20; i++) {
//                try {
//                    CheckJobsResults results = client.checkJobs(new CheckJobsParams().withJobIds(
//                            Arrays.asList(jobId, hiddenJobId))
//                            .withWithJobParams(1L));
//                    ret = results.getJobStates().get(jobId);
//                    hiddenJobError = results.getCheckError().get(hiddenJobId);
//                    System.out.println("Job finished: " + ret.getFinished());
//                    if (ret.getFinished() != null && ret.getFinished() == 1L) {
//                        break;
//                    }
//                    Thread.sleep(5000);
//                } catch (ServerException ex) {
//                    System.out.println(ex.getData());
//                    throw ex;
//                }
//            }
//            String errMsg = "Unexpected app state: " + UObject.getMapper().writeValueAsString(ret);
//            UObject obj = checkJobOutput(ret);
//            List<String> data = obj.asClassInstance(List.class);
//            Assert.assertEquals(errMsg, inputText, data.get(0));
//            Assert.assertEquals(errMsg, "OK", data.get(1));
//            Assert.assertNotNull(hiddenJobError);
//            Assert.assertEquals("AWE task wasn't found in DB for jobid=" + hiddenJobId,
//                    hiddenJobError.getMessage());
//        } catch (ServerException ex) {
//            System.err.println(ex.getData());
//            throw ex;
//        }
//    }

    public String lookupServiceVersion(String moduleName) throws Exception,
            IOException, InvalidFileFormatException, JsonClientException {
        CatalogClient cat = getCatalogClient(token, TesterUtils.loadConfig());
        String ver = cat.getModuleInfo(new SelectOneModuleParams().withModuleName(moduleName)).getDev().getGitCommitHash();
        return ver;
    }

    private static UserAndJobStateClient getUJSClient(
            final AuthToken token,
            final Map<String, String> config) throws Exception {
        final String ujsUrl = config.get(
                NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        final UserAndJobStateClient ujs = new UserAndJobStateClient(
                new URL(ujsUrl), token);
        ujs.setIsInsecureHttpConnectionAllowed(true);
        return ujs;
    }

    private static WorkspaceClient getWsClient(AuthToken auth,
                                               Map<String, String> config) throws Exception {
        String wsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
        WorkspaceClient ret = new WorkspaceClient(new URL(wsUrl), auth);
        ret.setIsInsecureHttpConnectionAllowed(true);
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

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties props = TesterUtils.props();
        token = TesterUtils.token(props);
        workDir = TesterUtils.prepareWorkDir(new File("temp_files"),
                "awe-integration");
        mongoDir = new File(workDir, "mongo");
        aweServerDir = new File(workDir, "awe_server");
        aweClientDir = new File(workDir, "awe_client");
        njsServiceDir = new File(workDir, "njs_service");
        File binDir = new File(njsServiceDir, "bin");
        String authUrl = TesterUtils.loadConfig().get(NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL);

        /**
        if(authUrl.contains("localhost") || authUrl.contains("nginx")) {
            catalogWrapper = startupCatalogWrapper();
        }
         **/
        String machineName = java.net.InetAddress.getLocalHost().getHostName();
        machineName = machineName == null ? "nowhere" : machineName.toLowerCase().replaceAll("[^\\dA-Za-z_]|\\s", "_");
        long suf = System.currentTimeMillis();
        WorkspaceClient wscl = getWsClient(token, TesterUtils.loadConfig());
        Exception error = null;
        for (int i = 0; i < 5; i++) {
            testWsName = "test_awe_docker_job_script_" + machineName + "_" + suf;
            try {
                testWsID = wscl.createWorkspace(new CreateWorkspaceParams()
                        .withWorkspace(testWsName)).getE1();
                error = null;
                break;
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                error = ex;
            }
        }
        if (error != null) {
            throw error;
        }
        //PUT REGISTER MODULE HERE BEFORE THIS WORKS
        registerTypes();
        stageWSObjects();

        String njs_url = props.getProperty("njs_server_url");
        client = new NarrativeJobServiceClient(new URL(njs_url), token);
        client.setIsInsecureHttpConnectionAllowed(true);
    }

    private static void stageWSObjects() throws Exception {
        WorkspaceClient wsc = getWsClient(token, TesterUtils.loadConfig());
        Map<String, String> mt = new HashMap<String, String>();
        List<Tuple11<Long, String, String, String, Long, String, Long, String,
                String, Long, Map<String, String>>> ret =
                wsc.saveObjects(new SaveObjectsParams()
                        .withWorkspace(testWsName)
                        .withObjects(Arrays.asList(
                                new ObjectSaveData()
                                        .withData(new UObject(mt))
                                        .withName(STAGED1_NAME)
                                        .withType("Empty.AType"),
                                new ObjectSaveData()
                                        .withData(new UObject(mt))
                                        .withName(STAGED2_NAME)
                                        .withType("Empty.AType")
                                )
                        ));
        NAME2REF.put(STAGED1_NAME, ret.get(0).getE7() + "/" +
                ret.get(0).getE1() + "/" + ret.get(0).getE5());
        NAME2REF.put(STAGED2_NAME, ret.get(1).getE7() + "/" +
                ret.get(1).getE1() + "/" + ret.get(1).getE5());
    }

    private static String findAweBinary(String program) throws Exception {
        String dirText = TesterUtils.props().getProperty("test-awe-bin-dir");
        if (dirText != null && !dirText.trim().isEmpty()) {
            File ret = new File(new File(dirText), program);
            if (ret.exists()) {
                return ret.getAbsolutePath();
            }
        }
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
        if (mongo != null) {
            mongo.destroy(false);
        }
        killPid(aweClientDir);
        killPid(aweServerDir);
        //killPid(shockDir);
        try {
            if (testWsName != null) {
                getWsClient(token, TesterUtils.loadConfig()).deleteWorkspace(
                        new WorkspaceIdentity().withWorkspace(testWsName));
                //System.out.println("Test workspace " + testWsName + " was deleted");
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    @After
    public void after() {
        System.out.println();
    }

    private static int startupAweServer(String aweServerExePath, File dir, int mongoPort,
                                        String authUrl, AuthToken token) throws Exception {
        //auth-service-url = https://ci.kbase.us/auth2services/auth/api/legacy/KBase/Sessions/Login
        //globus_token_url = https://ci.kbase.us/auth2services/auth/api/legacy/globus/goauth/token?grant_type=client_credentials
        //globus_profile_url = https://ci.kbase.us/auth2services/auth/api/legacy/globus/users
        String globusUrl = "https://nexus.api.globusonline.org";
        if (authUrl != null && authUrl.endsWith("KBase/Sessions/Login")) {
            globusUrl = authUrl.replace("KBase/Sessions/Login", "globus");
        }
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
        File siteJsDir = new File(siteDir, "js");
        siteJsDir.mkdir();
        writeFileLines(Arrays.asList("var RetinaConfig = {}"), new File(siteJsDir, "config.js.tt"));
        File awfDir = new File(dir, "awfs");
        awfDir.mkdir();
        int port = findFreePort();
        File configFile = new File(dir, "awe.cfg");
        writeFileLines(Arrays.asList(
                "[Admin]",
                "email=shock-admin@kbase.us",
                "users=" + token.getUserName(),
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
                "globus_token_url=" + globusUrl + "/goauth/token?grant_type=client_credentials",
                "globus_profile_url=" + globusUrl + "/users",
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
                "api-port=" + port,
                "[External]",
                "api-url=http://localhost:" + port + "/"
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
                "export AWE_CLIENTGROUP=test_client_group",
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
                                            int catalogPort, int mongoPort, AuthToken token) throws Exception {
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
        Map<String, String> origConfig = TesterUtils.loadConfig();
        List<String> configLines = new ArrayList<String>(Arrays.asList(
                "[" + NarrativeJobServiceServer.SERVICE_DEPLOYMENT_NAME + "]",
                NarrativeJobServiceServer.CFG_PROP_SCRATCH + "=" + dir.getAbsolutePath(),
                NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL),
                NarrativeJobServiceServer.CFG_PROP_SHOCK_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL),
                NarrativeJobServiceServer.CFG_PROP_AWE_SRV_URL + "=http://localhost:" + awePort + "/",
                NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL),
                NarrativeJobServiceServer.CFG_PROP_ADMIN_USER_NAME + "=kbasetest,rsutormin",
                NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL),
                NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL + "=http://localhost:" + catalogPort,
                NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT),
                NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL + "=http://localhost:" + port + "/",
                NarrativeJobServiceServer.CFG_PROP_REF_DATA_BASE + "=" + dir.getCanonicalPath(),
                NarrativeJobServiceServer.CFG_PROP_CATALOG_ADMIN_TOKEN + "=" + token.getToken(),
                NarrativeJobServiceServer.CFG_PROP_DEFAULT_AWE_CLIENT_GROUPS + "=kbase",
                NarrativeJobServiceServer.CFG_PROP_AWE_READONLY_ADMIN_TOKEN + "=" + token.getToken(),
                NarrativeJobServiceServer.CFG_PROP_MONGO_HOSTS + "=localhost:" + mongoPort,
                NarrativeJobServiceServer.CFG_PROP_MONGO_DBNAME + "=exec_engine",
                NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL + "=" + origConfig.get(NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL)
        ));
        String dockerURI = origConfig.get(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI);
        if (dockerURI != null)
            configLines.add(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI + "=" + dockerURI);
        String callbackNetworks = origConfig.get(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS);
        if (callbackNetworks != null)
            configLines.add(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS + "=" + callbackNetworks);
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

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {}
        throw new IllegalStateException("Can not find available port in system");
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
            Map<String, String> origConfig = TesterUtils.loadConfig();
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

        @JsonServerMethod(rpc = "Catalog.get_module_version")
        public ModuleVersion getModuleVersion(SelectModuleVersion selection) throws IOException, JsonClientException {
            return fwd().getModuleVersion(selection);
        }

        @JsonServerMethod(rpc = "Catalog.get_version_info")
        public ModuleVersionInfo getVersionInfo(SelectModuleVersionParams params) throws IOException, JsonClientException {
            return fwd().getVersionInfo(params);
        }

        @JsonServerMethod(rpc = "Catalog.log_exec_stats")
        public void logExecStats(LogExecStatsParams params, AuthToken authPart) throws IOException, JsonClientException {
            execStats.add(params);
        }

        @JsonServerMethod(rpc = "Catalog.list_client_group_configs")
        public List<ClientGroupConfig> listClientGroupConfigs(ClientGroupFilter filter) throws IOException, JsonClientException {
            return Arrays.asList(new ClientGroupConfig().withModuleName(filter.getModuleName())
                    .withFunctionName(filter.getFunctionName())
                    .withClientGroups(Arrays.asList("*")));
        }

        @JsonServerMethod(rpc = "Catalog.list_volume_mounts")
        public List<VolumeMountConfig> listVolumeMounts(VolumeMountFilter filter) throws IOException, JsonClientException {
            String userId = token.getUserName();
            if (filter.getModuleName().equals("onerepotest") && filter.getFunctionName().equals("list_ref_data") &&
                    filter.getClientGroup().equals("test_client_group") && new File(workDir, userId).exists()) {
                VolumeMountConfig ret = new VolumeMountConfig().withVolumeMounts(Arrays.asList(
                        new VolumeMount().withHostDir(workDir.getAbsolutePath() + "/${username}").withContainerDir("/kb/module/custom")));
                return Arrays.asList(ret);
            }
            return null;
        }

        @JsonServerMethod(rpc = "Catalog.get_secure_config_params")
        public List<SecureConfigParameter> getSecureConfigParams(GetSecureConfigParamsInput params, AuthToken authPart) throws IOException, JsonClientException {
            List<SecureConfigParameter> ret = new ArrayList<>();
            if (params.getModuleName().equals("onerepotest")) {
                ret.add(new SecureConfigParameter().withModuleName("onerepotest").withParamName("secret_password").withParamValue("Super password!"));
            }
            return ret;
        }
    }
}
