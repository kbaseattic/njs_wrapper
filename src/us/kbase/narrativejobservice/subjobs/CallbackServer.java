package us.kbase.narrativejobservice.subjobs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.common.service.JacksonTupleModule;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.UObject;
import us.kbase.common.utils.NetUtils;
import us.kbase.narrativejobservice.DockerRunner;
import us.kbase.narrativejobservice.RunJobParams;
import us.kbase.workspace.ProvenanceAction;
import us.kbase.workspace.SubAction;

public class CallbackServer extends JsonServerServlet {
    //TODO identical (or close to it) to kb_sdk call back server.
    // should probably go in java_common or make a common repo for shared
    // NJSW & KB_SDK code, since they're tightly coupled
    private static final long serialVersionUID = 1L;
    
    private final File mainJobDir;
    private final int callbackPort;
    private final Map<String, String> config;
    private final DockerRunner.LineLogger logger;
    private final ProvenanceAction prov = new ProvenanceAction();
    
    private final static DateTimeFormatter DATE_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZoneUTC();
    // IMPORTANT: don't access outside synchronized block
    private final static Map<String, ModuleRunVersion> vers =
            Collections.synchronizedMap(
                    new LinkedHashMap<String, ModuleRunVersion>());
    
    public CallbackServer(
            final File mainJobDir,
            final int callbackPort,
            final Map<String, String> config,
            final DockerRunner.LineLogger logger,
            final ModuleRunVersion runver,
            final RunJobParams job) {
        super("CallbackServer");
        this.mainJobDir = mainJobDir;
        this.callbackPort = callbackPort;
        this.config = config;
        this.logger = logger;
        vers.put(runver.getModule(), runver);
        prov.setTime(DATE_FORMATTER.print(new DateTime()));
        prov.setService(runver.getModule());
        prov.setMethod(runver.getMethod());
        prov.setDescription(
                "KBase SDK method run via the KBase Execution Engine");
        prov.setMethodParams(job.getParams());
        prov.setInputWsObjects(job.getSourceWsObjects());
        prov.setServiceVer(runver.getVersionAndRelease());
        initSilentJettyLogger();
    }
    
    @JsonServerMethod(rpc = "CallbackServer.get_provenance")
    public List<ProvenanceAction> getProvenance()
            throws IOException, JsonClientException {
        /* Would be more efficient if provenance was updated online
           although I can't imagine this making a difference compared to
           serialization / transport
         */
        final List<SubAction> sas = new LinkedList<SubAction>();
        for (final ModuleRunVersion mrv: vers.values()) {
           sas.add(new SubAction()
               .withCodeUrl(mrv.getGitURL().toExternalForm())
               .withCommit(mrv.getGitHash())
               .withName(mrv.getModuleDotMethod())
               .withVer(mrv.getVersionAndRelease()));
        }
        return new LinkedList<ProvenanceAction>(Arrays.asList(
                new ProvenanceAction()
                    .withSubactions(sas)
                    .withTime(prov.getTime())
                    .withService(prov.getService())
                    .withMethod(prov.getMethod())
                    .withDescription(prov.getDescription())
                    .withMethodParams(prov.getMethodParams())
                    .withInputWsObjects(prov.getInputWsObjects())
                    .withServiceVer(prov.getServiceVer())
                 ));
    }
    
    @JsonServerMethod(rpc = "CallbackServer.status")
    public UObject status() throws IOException, JsonClientException {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("state", "OK");
        return new UObject(data);
    }

    protected void processRpcCall(RpcCallData rpcCallData, String token, JsonServerSyslog.RpcInfo info, 
            String requestHeaderXForwardedFor, ResponseStatusSetter response, OutputStream output,
            boolean commandLine) {
        System.out.println("In CallbackServer.processRpcCall");
        if (rpcCallData.getMethod().startsWith("CallbackServer.")) {
            super.processRpcCall(rpcCallData, token, info, requestHeaderXForwardedFor, response, output, commandLine);
        } else {
            String rpcName = rpcCallData.getMethod();
            final String[] modMeth = rpcName.split("\\.");
            Map<String, Object> jsonRpcResponse = null;
            String errorMessage = null;
            ObjectMapper mapper = new ObjectMapper().registerModule(new JacksonTupleModule());
            try {
                if (modMeth.length != 2) {
                    throw new IllegalArgumentException("Illegal method name: " +
                            rpcName);
                }
                final String module = modMeth[0];
                final SubsequentCallRunner runner;
                synchronized(this) {
                    final String serviceVer;
                    if (vers.containsKey(module)) {
                        ModuleRunVersion v = vers.get(module);
                        serviceVer = v.getGitHash();
                        System.out.println(String.format(
                                "Warning: Module %s was already used once " +
                                        "for this job. Using cached " +
                                        "version:\n" +
                                        "url: %s\n" +
                                        "commit: %s\n" +
                                        "version: %s\n" +
                                        "release: %s\n",
                                        module, v.getGitURL(), v.getGitHash(),
                                        v.getVersion(), v.getRelease()));
                    } else {
                        serviceVer = rpcCallData.getContext() == null ? null : 
                            (String)rpcCallData.getContext()
                            .getAdditionalProperties().get("service_ver");
                    }
                    // Request docker image name from Catalog
                    runner = new SubsequentCallRunner(mainJobDir, rpcName,
                            serviceVer, callbackPort, config, logger);
                    if (!vers.containsKey(module)) {
                        vers.put(module, runner.getModuleRunVersion());
                    }
                }
                // Run method in local docker container
                jsonRpcResponse = runner.run(rpcCallData);
            } catch (Exception ex) {
                ex.printStackTrace();
                errorMessage = ex.getMessage();
            }
            try {
                if (jsonRpcResponse == null) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    if (errorMessage == null)
                        errorMessage = "Unknown server error";
                    Map<String, Object> error = new LinkedHashMap<String, Object>();
                    error.put("name", "JSONRPCError");
                    error.put("code", -32601);
                    error.put("message", errorMessage);
                    error.put("error", errorMessage);
                    jsonRpcResponse = new LinkedHashMap<String, Object>();
                    jsonRpcResponse.put("version", "1.1");
                    jsonRpcResponse.put("error", error);
                } else {
                    if (jsonRpcResponse.containsKey("error"))
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                mapper.writeValue(new UnclosableOutputStream(output), jsonRpcResponse);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public static String getCallbackUrl(int callbackPort) throws Exception {
        List<String> hostIps = NetUtils.findNetworkAddresses("docker0", "vboxnet0");
        String hostIp = null;
        if (hostIps.isEmpty()) {
            System.out.println("WARNING! No Docker host IP addresses was found. Subsequent local calls are not supported in test mode.");
        } else {
            hostIp = hostIps.get(0);
            if (hostIps.size() > 1) {
                System.out.println("WARNING! Several Docker host IP addresses are detected, first one is used: " + hostIp);
            } else {
                System.out.println("Docker host IP address is detected: " + hostIp);
            }
        }
        String callbackUrl = hostIp == null ? "" : ("http://" + hostIp + ":" + callbackPort);
        return callbackUrl;
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

    private static class UnclosableOutputStream extends OutputStream {
        OutputStream inner;
        boolean isClosed = false;
        
        public UnclosableOutputStream(OutputStream inner) {
            this.inner = inner;
        }
        
        @Override
        public void write(int b) throws IOException {
            if (isClosed)
                return;
            inner.write(b);
        }
        
        @Override
        public void close() throws IOException {
            isClosed = true;
        }
        
        @Override
        public void flush() throws IOException {
            inner.flush();
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            if (isClosed)
                return;
            inner.write(b);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (isClosed)
                return;
            inner.write(b, off, len);
        }
    }
}
