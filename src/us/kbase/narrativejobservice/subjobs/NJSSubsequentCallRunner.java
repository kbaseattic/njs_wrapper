package us.kbase.narrativejobservice.subjobs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import com.github.dockerjava.api.model.Bind;

import us.kbase.auth.AuthToken;
import us.kbase.common.executionengine.ModuleMethod;
import us.kbase.catalog.ModuleVersion;
import us.kbase.common.executionengine.SubsequentCallRunner;
import us.kbase.common.executionengine.CallbackServerConfigBuilder.CallbackServerConfig;
import us.kbase.common.service.JsonClientException;
import us.kbase.narrativejobservice.sdkjobs.CancellationChecker;
import us.kbase.narrativejobservice.sdkjobs.DockerRunner;

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
            final ModuleVersion moduleVersion,
            final AuthToken token)
            throws IOException, InterruptedException {
        final Path outputFile = getJobWorkDir(jobId, config, imageName)
                .resolve("output.json");
        config.getLogger().logNextLine("dockerURI=" + config.getDockerURI(),
                false);
        config.getLogger().logNextLine(
                "Running docker container for image: " + imageName, false);
        final Path sharedScratchDir = getSharedScratchDir(config);
        // Create a refdata path in the format 'dataBase/dataFolder/dataVersion'
        File refDataDir = null;
        if (moduleVersion.getDataFolder() != null && moduleVersion.getDataVersion() != null) {
            refDataDir = Paths.get(
                config.refDataBase.toString(),
                moduleVersion.getDataFolder(),
                moduleVersion.getDataVersion()
            ).toFile();
        }
        new DockerRunner(config.getDockerURI()).run(
                imageName, moduleName, inputFile.toFile(), token,
                config.getLogger(), outputFile.toFile(), false, refDataDir,
                sharedScratchDir.toFile(), config.getCallbackURL(),
                jobId.toString(), additionalBinds, cancellationChecker, null);
        return outputFile;
    }
    
}
