package us.kbase.narrativejobservice.subjobs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import us.kbase.common.service.JacksonTupleModule;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;
import us.kbase.common.service.JsonServerSyslog;
import us.kbase.common.service.RpcContext;
import us.kbase.common.service.UObject;
import us.kbase.common.utils.ModuleMethod;
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
    
    private static final int MAX_JOBS = 10;
    
    private final File mainJobDir;
    private final int callbackPort;
    private final Map<String, String> config;
    private final DockerRunner.LineLogger logger;
    private final ProvenanceAction prov = new ProvenanceAction();
    
    private final static DateTimeFormatter DATE_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZoneUTC();
    // IMPORTANT: don't access outside synchronized block
    private final Map<String, ModuleRunVersion> vers =
            Collections.synchronizedMap(
                    new LinkedHashMap<String, ModuleRunVersion>());
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    private final Map<UUID, FutureTask<Map<String, Object>>> runningJobs =
            new HashMap<UUID, FutureTask<Map<String, Object>>>();
    
    private final Cache<UUID, FutureTask<Map<String, Object>>> resultsCache =
            CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    
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
        vers.put(runver.getModuleMethod().getModule(), runver);
        prov.setTime(DATE_FORMATTER.print(new DateTime()));
        prov.setService(runver.getModuleMethod().getModule());
        prov.setMethod(runver.getModuleMethod().getMethod());
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
               .withName(mrv.getModuleMethod().getModuleDotMethod())
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

    private static void cbLog(String log) {
        System.out.println((System.currentTimeMillis() / 1000.0) +
                " - CallbackServer: " + log);
    }
    
    protected void processRpcCall(RpcCallData rpcCallData, String token, JsonServerSyslog.RpcInfo info, 
            String requestHeaderXForwardedFor, ResponseStatusSetter response, OutputStream output,
            boolean commandLine) {
        cbLog("In processRpcCall");
        if (rpcCallData.getMethod().startsWith("CallbackServer.")) {
            super.processRpcCall(rpcCallData, token, info, requestHeaderXForwardedFor, response, output, commandLine);
        } else {
            String errorMessage = null;
            Map<String, Object> jsonRpcResponse = null;
            try {
                jsonRpcResponse = handleCall(rpcCallData);
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
                final ObjectMapper mapper = new ObjectMapper().registerModule(
                        new JacksonTupleModule());
                mapper.writeValue(new UnclosableOutputStream(output), jsonRpcResponse);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Map<String, Object> handleCall(
            final RpcCallData rpcCallData) throws IOException,
            JsonClientException, InterruptedException {

        final ModuleMethod modmeth = new ModuleMethod(
                rpcCallData.getMethod());
        final Map<String, Object> jsonRpcResponse;

        if (modmeth.isCheck()) {
            jsonRpcResponse = runCheck(rpcCallData);
        } else {
            final UUID jobId = UUID.randomUUID();
            cbLog(String.format("Subjob method: %s ID: %s",
                    modmeth.getModuleDotMethod(), jobId));
            final SubsequentCallRunner runner = getJobRunner(
                    jobId, rpcCallData.getContext(), modmeth);
            
            // update method name to get rid of suffixes
            rpcCallData.setMethod(modmeth.getModuleDotMethod());
            
            if (modmeth.isStandard()) {
                cbLog(String.format(
                        "Warning: the callback server recieved a " +
                        "request to synchronously run the method " +
                        "%s. The callback server will block until " +
                        "the method is completed.",
                        modmeth.getModuleDotMethod()));
                // Run method in local docker container
                jsonRpcResponse = runner.run(rpcCallData);
            } else {
                final FutureTask<Map<String, Object>> task =
                        new FutureTask<Map<String, Object>>(
                                new SubsequentCallRunnerRunner(
                                        runner, rpcCallData));
                synchronized(this) {
                    if (runningJobs.size() >= MAX_JOBS) {
                        throw new IllegalStateException(String.format(
                                "No more than %s concurrent methods " +
                                        "are allowed", MAX_JOBS));
                    }
                    executor.execute(task);
                    runningJobs.put(jobId,task);
                }
                jsonRpcResponse = new HashMap<String, Object>();
                jsonRpcResponse.put("version", "1.1");
                jsonRpcResponse.put("id", rpcCallData.getId());
                jsonRpcResponse.put("result", Arrays.asList(jobId));
            }
        }
        return jsonRpcResponse;
    }

    private Map<String, Object> runCheck(final RpcCallData rpcCallData)
            throws InterruptedException, IOException {
        
        final Map<String, Object> resp;
        int finished = 1;
        //TODO putting finished in the top level breaks the jsonrpc spec
        // need to think about how this should work
        // putting the context object also breaks the spec
        if (!(rpcCallData.getParams().size() == 1)) {
            throw new IllegalArgumentException(
                    "Check methods only take one argument");
        }
        final UUID jobId = UUID.fromString(rpcCallData.getParams().get(0)
                .asClassInstance(String.class));
        synchronized (this) {
            if (runningJobs.containsKey(jobId)) {
                if (runningJobs.get(jobId).isDone()) {
                    final FutureTask<Map<String, Object>> task =
                            runningJobs.get(jobId);
                    resultsCache.put(jobId, task);
                    runningJobs.remove(jobId);
                    resp = getResults(task);
                } else {
                    resp = new HashMap<String, Object>();
                    resp.put("version", "1.1");
                    resp.put("id", rpcCallData.getId());
                    finished = 0;
                }
            } else {
                final FutureTask<Map<String, Object>> task =
                        resultsCache.getIfPresent(jobId);
                if (task == null) {
                    throw new IllegalStateException(
                            "Either there is no job with id " + jobId +
                            "or it has expired from the cache");
                }
                resp = getResults(task);
            }
        }
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put("finished", finished);
        if (resp.containsKey("result")) { // otherwise an error occured
            result.put("result", resp.get("result"));
        }
        resp.put("result", result);
        return resp;
    }

    private Map<String, Object> getResults(
            final FutureTask<Map<String, Object>> task)
            throws InterruptedException, IOException {
        try {
            return task.get();
        } catch (ExecutionException ee) {
            final Throwable cause = ee.getCause();
            if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            } else if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw (Error) cause;
            }
        }
    }

    private static class SubsequentCallRunnerRunner
            implements Callable<Map<String, Object>> {

        private final SubsequentCallRunner scr;
        private final RpcCallData rpc;

        public SubsequentCallRunnerRunner(
                final SubsequentCallRunner scr,
                final RpcCallData rpcData) {
            this.scr = scr;
            this.rpc = rpcData;
        }
        

        @Override
        public Map<String, Object> call() throws Exception {
            return scr.run(rpc);
        }
        
    }
    
    private SubsequentCallRunner getJobRunner(
            final UUID jobId,
            final RpcContext rpcContext,
            final ModuleMethod modmeth)
            throws IOException, JsonClientException  {
        final SubsequentCallRunner runner;
        synchronized(this) {
            final String serviceVer;
            if (vers.containsKey(modmeth.getModule())) {
                ModuleRunVersion v = vers.get(modmeth.getModule());
                serviceVer = v.getGitHash();
                cbLog(String.format(
                        "Warning: Module %s was already used once " +
                                "for this job. Using cached " +
                                "version:\n" +
                                "url: %s\n" +
                                "commit: %s\n" +
                                "version: %s\n" +
                                "release: %s\n",
                                modmeth.getModule(), v.getGitURL(),
                                v.getGitHash(), v.getVersion(),
                                v.getRelease()));
            } else {
                serviceVer = rpcContext == null ? null : 
                    (String)rpcContext.getAdditionalProperties()
                        .get("service_ver");
            }
            // Request docker image name from Catalog
            runner = new SubsequentCallRunner(
                    jobId, mainJobDir, modmeth,
                    serviceVer, callbackPort, config, logger);
            if (!vers.containsKey(modmeth.getModule())) {
                vers.put(modmeth.getModule(), runner.getModuleRunVersion());
            }
        }
        return runner;
    }
    
    public static String getCallbackUrl(int callbackPort)
            throws SocketException {
        List<String> hostIps = NetUtils.findNetworkAddresses("docker0", "vboxnet0");
        String hostIp = null;
        if (hostIps.isEmpty()) {
            cbLog("WARNING! No Docker host IP addresses was found. Subsequent local calls are not supported in test mode.");
        } else {
            hostIp = hostIps.get(0);
            if (hostIps.size() > 1) {
                cbLog("WARNING! Several Docker host IP addresses are detected, first one is used: " + hostIp);
            } else {
                cbLog("Docker host IP address is detected: " + hostIp);
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
