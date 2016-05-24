package us.kbase.narrativejobservice.subjobs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.utils.ModuleMethod;
import us.kbase.narrativejobservice.DockerRunner;
import us.kbase.narrativejobservice.subjobs.CallbackServerConfigBuilder.CallbackServerConfig;

public class NJSSubsequentCallRunner extends SubsequentCallRunner {

    public NJSSubsequentCallRunner(
            final AuthToken token,
            final CallbackServerConfig config,
            final UUID jobId,
            final ModuleMethod modmeth,
            final String serviceVer)
            throws IOException, JsonClientException, TokenFormatException {
        super(token, config, jobId, modmeth, serviceVer);
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
        new DockerRunner(config.getDockerURI()).run(
                imageName, moduleName, inputFile.toFile(), token,
                config.getLogger(), outputFile.toFile(), false, null,
                sharedScratchDir.toFile(), config.getCallbackURL());
        return outputFile;
    }
    
}
