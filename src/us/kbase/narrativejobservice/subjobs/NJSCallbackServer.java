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
import us.kbase.common.executionengine.CallbackServer;
import us.kbase.common.executionengine.CallbackServerConfigBuilder;
import us.kbase.common.executionengine.LineLogger;
import us.kbase.common.executionengine.ModuleMethod;
import us.kbase.common.executionengine.ModuleRunVersion;
import us.kbase.common.executionengine.SubsequentCallRunner;
import us.kbase.common.executionengine.CallbackServerConfigBuilder.CallbackServerConfig;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;
import us.kbase.narrativejobservice.sdkjobs.CancellationChecker;

public class NJSCallbackServer extends CallbackServer {

    private static final long serialVersionUID = 1L;

    protected final List<Bind> additionalBinds;
    protected final CancellationChecker cancellationChecker;
    
    public NJSCallbackServer(
            final AuthToken token,
            final CallbackServerConfig config,
            final ModuleRunVersion runver,
            final List<UObject> methodParameters,
            final List<String> inputWorkspaceObjects,
            final List<Bind> additionalBinds,
            final CancellationChecker cancellationChecker) {
        super(token, config, runver, methodParameters, inputWorkspaceObjects);
        this.additionalBinds = additionalBinds;
        this.cancellationChecker = cancellationChecker;
    }

    @Override
    protected SubsequentCallRunner createJobRunner(
            final AuthToken token,
            final CallbackServerConfig config,
            final UUID jobId,
            final ModuleMethod modmeth,
            final String serviceVer)
            throws IOException, JsonClientException {
        return new NJSSubsequentCallRunner(token, config,
                jobId, modmeth, serviceVer, additionalBinds, 
                cancellationChecker);
    }
}
