package us.kbase.narrativejobservice.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.UObject;
import us.kbase.common.utils.ProcessHelper;
import us.kbase.common.utils.TextUtils;
import us.kbase.narrativejobservice.AweClientDockerJobScript;
import us.kbase.narrativejobservice.JobState;
import us.kbase.narrativejobservice.NarrativeJobServiceClient;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.narrativejobservice.RunJobParams;
import us.kbase.shock.client.BasicShockClient;
import us.kbase.shock.client.ShockNodeId;
import us.kbase.userandjobstate.UserAndJobStateClient;

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
    private static Server njsService = null;
    private static String testWsName = null;
    
    private static int mongoInitWaitSeconds = 120;
    private static int aweServerInitWaitSeconds = 60;
    private static int aweClientInitWaitSeconds = 60;
    private static int njsJobWaitSeconds = 60;
    
    private static final String wsUrl = "https://ci.kbase.us/services/ws/";
    private static final String ujsUrl = "https://ci.kbase.us/services/userandjobstate/";
    private static final String shockUrl = "https://ci.kbase.us/services/shock-api";
    private static final String dockerRegUrl = "dockerhub-ci.kbase.us";
    
    @Test
    public void mainTest() throws Exception {
        /*Map<String, String> config = new LinkedHashMap<String, String>();
        config.put(NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL, dockerRegUrl);
        config.put(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_SCRATCH, njsServiceDir.getAbsolutePath());
        config.put(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL, ujsUrl);
        config.put(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL, shockUrl);
        UserAndJobStateClient ujsClient = getUjsClient(token, config);
        BasicShockClient shockClient = getShockClient(token, config);
        */
        RunJobParams params = new RunJobParams().withMethod(
                "GenomeFeatureComparator.compare_genome_features")
                .withParams(Arrays.asList(UObject.fromJsonString(
                        "{\"genomeA\":\"myws.mygenome1\",\"genomeB\":\"myws.mygenome2\"}")));
        String jobId = client.runJob(params);
        for (int i = 0; i < 100; i++) {
            JobState ret = client.checkJob(jobId);
            System.out.println(UObject.getMapper().writeValueAsString(ret));
            if (ret.getFinished() != null && ret.getFinished() == 1L) {
                break;
            }
            Thread.sleep(1000);
        }
        /*ByteArrayOutputStream baos = new ByteArrayOutputStream();
        UObject.getMapper().writeValue(baos, params);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        String inputShockId = shockClient.addNode(bais, "job.json", "json").getId().getId();
        String ujsJobId = ujsClient.createJob();
        String outputShockId = shockClient.addNode().getId().getId();
        System.setProperty("KB_AUTH_TOKEN", token.toString());
        AweClientDockerJobScript.main(new String[] {ujsJobId, inputShockId, outputShockId, 
                TextUtils.stringToHex(UObject.getMapper().writeValueAsString(config))});
        baos = new ByteArrayOutputStream();
        shockClient.getFile(new ShockNodeId(outputShockId), baos);
        baos.close();
        System.out.println(new String(baos.toByteArray()));*/
    }

    private static UserAndJobStateClient getUjsClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        String jobSrvUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        UserAndJobStateClient ret = new UserAndJobStateClient(new URL(jobSrvUrl), auth);
        ret.setIsInsecureHttpConnectionAllowed(true);
        ret.setAllSSLCertificatesTrusted(true);
        return ret;
    }

    private static BasicShockClient getShockClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        String shockUrl = config.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL);
        BasicShockClient ret = new BasicShockClient(new URL(shockUrl), auth);
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
        njsService = startupNJSService(njsServiceDir, binDir, awePort);
        int jobServicePort = njsService.getConnectors()[0].getLocalPort();
        startupAweClient(findAweBinary(aweBinDir, "awe-client"), aweClientDir, awePort, binDir);
        client = new NarrativeJobServiceClient(new URL("http://localhost:" + jobServicePort), token);
        client.setIsInsecureHttpConnectionAllowed(true);
        /*String machineName = java.net.InetAddress.getLocalHost().getHostName();
        machineName = machineName == null ? "nowhere" : machineName.toLowerCase().replaceAll("[^\\dA-Za-z_]|\\s", "_");
        long suf = System.currentTimeMillis();
        WorkspaceClient wscl = getWsClient();
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
            throw error;*/
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
                "users=",
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

    private static Server startupNJSService(File dir, File binDir, int awePort) throws Exception {
        if (!dir.exists())
            dir.mkdirs();
        if (!binDir.exists())
            binDir.mkdirs();
        ProcessHelper.cmd("ant", "script", "-Djardir=" + dir.getAbsolutePath(), 
                "-Dbindir=" + binDir.getAbsolutePath()).exec(new File("."));
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
        File configFile = new File(dir, "deploy.cfg");
        int port = findFreePort();
        writeFileLines(Arrays.asList(
                "[" + NarrativeJobServiceServer.SERVICE_DEPLOYMENT_NAME + "]",
                NarrativeJobServiceServer.CFG_PROP_SCRATCH + "=" + dir.getAbsolutePath(),
                NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL + "=" + ujsUrl,
                NarrativeJobServiceServer.CFG_PROP_SHOCK_URL + "=" + shockUrl,
                NarrativeJobServiceServer.CFG_PROP_QUEUE_DB_DIR + "=" + new File(dir, "queue").getAbsolutePath(),
                NarrativeJobServiceServer.CFG_PROP_AWE_SRV_URL + "=http://localhost:" + awePort + "/",
                NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL + "=" + dockerRegUrl,
                NarrativeJobServiceServer.CFG_PROP_RUNNING_TASKS_PER_USER + "=5",
                NarrativeJobServiceServer.CFG_PROP_THREAD_COUNT + "=2",
                NarrativeJobServiceServer.CFG_PROP_REBOOT_MODE + "=false",
                NarrativeJobServiceServer.CFG_PROP_ADMIN_USER_NAME + "=kbasetest",
                NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL + "=" + wsUrl
                ), configFile);
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
                if (ex.getMessage().contains("Can not find method [Unknown] in server class")) {
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
    
    private static File prepareWorkDir(String testName) throws IOException {
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
}
