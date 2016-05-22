package us.kbase.narrativejobservice.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JacksonTupleModule;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.UObject;
import us.kbase.common.test.controllers.ControllerCommon;
import us.kbase.common.utils.ModuleMethod;
import us.kbase.narrativejobservice.DockerRunner;
import us.kbase.narrativejobservice.subjobs.CallbackServer;
import us.kbase.narrativejobservice.subjobs.CallbackServerConfigBuilder;
import us.kbase.narrativejobservice.subjobs.ModuleRunVersion;
import us.kbase.narrativejobservice.subjobs.CallbackServerConfigBuilder.CallbackServerConfig;
import us.kbase.workspace.ProvenanceAction;

public class CallbackServerTest {

    public static final Path TEST_DIR = Paths.get("temp_test_callback");
    
    public static AuthToken token;

    @BeforeClass
    public static void beforeClass() throws Exception {
        FileUtils.deleteDirectory(TEST_DIR.toFile());
        Files.deleteIfExists(TEST_DIR);
        Files.createDirectories(TEST_DIR);
        final Properties props = TesterUtils.props();
        token =  AuthService.login(get(props, "user"),
                get(props, "password")).getToken();
        
    }

    private static CallbackStuff startCallBackServer(
            final ModuleRunVersion runver,
            final List<UObject> params,
            final List<String> wsobjs)
            throws Exception {
        final DockerRunner.LineLogger log = new DockerRunner.LineLogger() {
            
            @Override
            public void logNextLine(String line, boolean isError) {
                System.out.println("Docker logger std" +
                        (isError ? "err" : "out") + ": " + line);
                
            }
        };
        final int callbackPort = ControllerCommon.findFreePort();
        final URL callbackUrl = CallbackServer.getCallbackUrl(callbackPort);
        final Path temp = Files.createTempDirectory(TEST_DIR, "cbt");
        final CallbackServerConfig cbcfg =
                new CallbackServerConfigBuilder(
                AweClientDockerJobScriptTest.loadConfig(), callbackUrl,
                        temp, log).build();
        final JsonServerServlet callback = new CallbackServer(
                token, cbcfg, runver, params, wsobjs);
        final Server callbackServer = new Server(callbackPort);
        final ServletContextHandler srvContext =
                new ServletContextHandler(
                        ServletContextHandler.SESSIONS);
        srvContext.setContextPath("/");
        callbackServer.setHandler(srvContext);
        srvContext.addServlet(new ServletHolder(callback),"/*");
        callbackServer.start();
        Thread.sleep(1000);
        return new CallbackStuff(callbackUrl, temp, callbackServer);
    }
    
    private static class CallbackStuff {
        final public URL callbackURL;
        final public Path tempdir;
        final public Server server;
        
        final private ObjectMapper mapper = new ObjectMapper().registerModule(
                new JacksonTupleModule());

        private CallbackStuff(URL callbackURL, Path tempdir,
                Server server) {
            super();
            this.callbackURL = callbackURL;
            this.tempdir = tempdir;
            this.server = server;
        }
        
        public List<ProvenanceAction> getProvenance() throws Exception {
            final TypeReference<List<ProvenanceAction>> retType =
                    new TypeReference<List<ProvenanceAction>>() {};
            final List<Object> arg = new ArrayList<Object>();
            final String method = "CallbackServer.get_provenance";
            
            return callServer(method, arg, null, retType);
        }
        
        public Map<String, Object> callMethod(
                final String method,
                final Map<String, Object> params,
                final String serviceVer)
                throws Exception {
            return callServer(method, Arrays.asList(params), serviceVer,
                    new TypeReference<Map<String,Object>>() {});
        }

        private <RET> RET callServer(
                final String method,
                final List<?> args,
                final String serviceVer,
                final TypeReference<RET> retType)
                throws Exception {
            final HttpURLConnection conn =
                    (HttpURLConnection) callbackURL.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            final OutputStream os = conn.getOutputStream();
            JsonGenerator g = mapper.getFactory().createGenerator(
                    os, JsonEncoding.UTF8);
            g.writeStartObject();
            final String id = ("" + Math.random()).replace(".", "");
            g.writeObjectField("params", args);
            g.writeStringField("method", method);
            g.writeStringField("version", "1.1");
            g.writeStringField("id", id);
            if (serviceVer != null) {
                g.writeObjectField("context",
                        ImmutableMap.<String, String>builder()
                            .put("service_ver", serviceVer).build());
            }
            g.writeEndObject();
            g.close();
            os.flush();
            final int code = conn.getResponseCode();
            if (code == 500) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> msg = mapper.readValue(
                        conn.getErrorStream(), Map.class);
                @SuppressWarnings("unchecked")
                final Map<String, Object> err =
                        (Map<String, Object>) msg.get("error"); 
                final String data = (String) (err.get("data") == null ?
                        err.get("error") : err.get("data"));
                throw new ServerException((String) err.get("message"),
                        (Integer) err.get("code"), (String) err.get("name"),
                        data);
            } else {
                @SuppressWarnings("unchecked")
                final Map<String, Object> msg = mapper.readValue(
                        conn.getInputStream(), Map.class);
                @SuppressWarnings("unchecked")
                final List<List<Object>> ret =
                        (List<List<Object>>) msg.get("result");
                RET res = UObject.transformObjectToObject(
                        ret.get(0), retType);
                return res;
            }
        }
    }
    
    private static String get(Properties props, String propName) {
        String ret = props.getProperty(propName);
        if (ret == null)
            throw new IllegalStateException("Property is not defined: " +
                    propName);
        return ret;
    }
    
    @Test
    public void sometest() throws Exception {
        final ModuleRunVersion runver = new ModuleRunVersion(
                new URL("https://foo.com"), new ModuleMethod("foo.bar"),
                "githash", "0.0.5", "dev");
        final List<UObject> params = new LinkedList<UObject>();
        final List<String> wsobjs = new ArrayList<String>();
        final CallbackStuff res =
                startCallBackServer(runver, params, wsobjs);
        System.out.println("Running sometest in dir " + res.tempdir);
        ProvenanceAction pa = res.getProvenance().get(0);
        System.out.println("serv " + pa.getService());
        System.out.println("code url " + pa.getSubactions().get(0).getCodeUrl());
        
        Map<String, Object> r = null;
        try {
            r = res.callMethod("njs_sdk_test_1.run",
                    ImmutableMap.<String, Object>builder()
                        .put("wait", 1)
                        .put("id", "whee").build(), "dev");
        } catch (ServerException se) {
            System.out.println("err" + se);
            System.out.println("data\n" + se.getData());
        }
        System.out.println(r);
        
        res.server.stop();
    }

}
