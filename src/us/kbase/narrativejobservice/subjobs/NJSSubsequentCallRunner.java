package us.kbase.narrativejobservice.subjobs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.github.dockerjava.api.model.Bind;

import us.kbase.auth.AuthToken;
import us.kbase.common.executionengine.ModuleMethod;
import us.kbase.common.executionengine.SubsequentCallRunner;
import us.kbase.common.executionengine.CallbackServerConfigBuilder.CallbackServerConfig;
import us.kbase.common.service.JsonClientException;
import us.kbase.narrativejobservice.sdkjobs.CancellationChecker;
import us.kbase.narrativejobservice.sdkjobs.DockerRunner;
import us.kbase.narrativejobservice.sdkjobs.ShifterRunner;


public class NJSSubsequentCallRunner extends SubsequentCallRunner {
    protected final List<Bind> additionalBinds;
    protected final CancellationChecker cancellationChecker;

    public NJSSubsequentCallRunner(
            final AuthToken token,
            final CallbackServerConfig config,
            final UUID jobId,
            final ModuleMethod modmeth,
            final String serviceVer,
            final List<Bind> additionalBinds,
            final CancellationChecker cancellationChecker)
            throws IOException, JsonClientException {
        super(token, config, jobId, modmeth, serviceVer);
        this.additionalBinds = additionalBinds;
        this.cancellationChecker = cancellationChecker;
    }

    @Override
    protected Path runModule(
            final UUID jobId,
            final Path inputFile,
            final CallbackServerConfig config,
            final String imageName,
            final String moduleName,
            final AuthToken token)
            throws IOException, InterruptedException {
        final Path outputFile = getJobWorkDir(jobId, config, imageName)
                .resolve("output.json");
        config.getLogger().logNextLine("dockerURI=" + config.getDockerURI(),
                false);
        config.getLogger().logNextLine(
                "Running docker container for image: " + imageName, false);
        final Path sharedScratchDir = getSharedScratchDir(config);
        if (System.getenv("USE_SHIFTER")!=null)
            new ShifterRunner(config.getDockerURI()).run(
                    imageName, moduleName, inputFile.toFile(), token,
                    config.getLogger(), outputFile.toFile(), null,
                    sharedScratchDir.toFile(), config.getCallbackURL(),
                    jobId.toString(), additionalBinds,
                    cancellationChecker, null);
        else
            new DockerRunner(config.getDockerURI()).run(
                    imageName, moduleName, inputFile.toFile(), token,
                    config.getLogger(), outputFile.toFile(), false, null,
                    sharedScratchDir.toFile(), config.getCallbackURL(),
                    jobId.toString(), additionalBinds, cancellationChecker, null);
        return outputFile;
    }

}
