package us.kbase.narrativejobservice.subjobs;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.github.dockerjava.api.model.Bind;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.common.executionengine.CallbackServer;
import us.kbase.common.executionengine.CallbackServerConfigBuilder;
import us.kbase.common.executionengine.LineLogger;
import us.kbase.common.executionengine.ModuleMethod;
import us.kbase.common.executionengine.ModuleRunVersion;
import us.kbase.common.executionengine.SubsequentCallRunner;
import us.kbase.common.executionengine.CallbackServerConfigBuilder.CallbackServerConfig;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;

public class NJSCallbackServer extends CallbackServer {

    private static final long serialVersionUID = 1L;

    protected final List<Bind> additionalBinds;
    
    public NJSCallbackServer(
            final AuthToken token,
            final CallbackServerConfig config,
            final ModuleRunVersion runver,
            final List<UObject> methodParameters,
            final List<String> inputWorkspaceObjects,
            final List<Bind> additionalBinds) {
        super(token, config, runver, methodParameters, inputWorkspaceObjects);
        this.additionalBinds = additionalBinds;
    }

    @Override
    protected SubsequentCallRunner createJobRunner(
            final AuthToken token,
            final CallbackServerConfig config,
            final UUID jobId,
            final ModuleMethod modmeth,
            final String serviceVer)
            throws IOException, JsonClientException, TokenFormatException {
        return new NJSSubsequentCallRunner(token, config,
                jobId, modmeth, serviceVer, additionalBinds);
    }
    
    public static void main(final String[] args) throws Exception {
        final AuthToken token = AuthService.login(args[0], args[1]).getToken();
        int port = 10000;
        CallbackServerConfig cfg = new CallbackServerConfigBuilder(
                new URL("https://ci.kbase.us/services/"),
                getCallbackUrl(port),
                Paths.get("temp_CallbackServer"),
                new LineLogger() {
                    @Override
                    public void logNextLine(String line, boolean isError) {
                        cbLog("Docker logger std" + (isError ? "err" : "out") +
                                ": " + line);
                    }
                })
                .withDockerURI(new URI("unix:///var/run/docker.sock"))
                .build();

        ModuleRunVersion runver = new ModuleRunVersion(
                new URL("https://github.com/mcreosote/foo"),
                new ModuleMethod("foo.bar"), "hash", "1034.1.0", "dev");
        
        NJSCallbackServer serv = new NJSCallbackServer(token, cfg, runver,
                new LinkedList<UObject>(), new LinkedList<String>(), null);
        
        new Thread(new CallbackRunner(serv, port)).start();
        System.out.println("Started on port " + port);
        System.out.println("workdir: " + cfg.getWorkDir());
    }
}
