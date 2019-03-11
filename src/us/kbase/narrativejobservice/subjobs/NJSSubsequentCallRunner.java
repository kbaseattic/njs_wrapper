package us.kbase.narrativejobservice.subjobs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.dockerjava.api.model.Bind;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.apache.commons.io.FileUtils;
import us.kbase.auth.AuthToken;
import us.kbase.common.executionengine.ModuleMethod;
import us.kbase.catalog.ModuleVersion;
import us.kbase.common.executionengine.SubsequentCallRunner;
import us.kbase.common.executionengine.CallbackServerConfigBuilder.CallbackServerConfig;
import us.kbase.common.service.JsonClientException;
import us.kbase.narrativejobservice.sdkjobs.CancellationChecker;
import us.kbase.narrativejobservice.sdkjobs.DockerRunner;
import us.kbase.narrativejobservice.sdkjobs.ShifterRunner;
import us.kbase.narrativejobservice.sdkjobs.SDKJobsUtils;


public class NJSSubsequentCallRunner extends SubsequentCallRunner {
    protected final List<Bind> additionalBinds;
    protected final CancellationChecker cancellationChecker;



    /**
     * Used for getting PID
     */
    private interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);
        int getpid ();
    }



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

        Map<String,String> labels = new HashMap<>();
        labels.put("runner_job_id",""+jobId);
        labels.put("job_id",""+ System.getenv("UJS_JOB_ID"));
        labels.put("condor_id",""+ System.getenv("CONDOR_ID"));
        labels.put("image_name",imageName);
        labels.put("module_name",moduleName);
        labels.put("module_version",moduleVersion.getVersion());
        labels.put("user_name",token.getUserName());

        String cgroupParent = null;
        try{
            cgroupParent = new SDKJobsUtils().lookupParentCgroup(CLibrary.INSTANCE.getpid());
        }
        catch (Exception e){
            System.out.println("Couldn't retrieve cgroupParent for pid.");
        }


        if (System.getenv("USE_SHIFTER")!=null){
            // TODO: Add refdata
            // TODO: Add cgroups
            new ShifterRunner(config.getDockerURI()).run(
                    imageName, moduleName, inputFile.toFile(), token,
                    config.getLogger(), outputFile.toFile(), false, refDataDir,
                    sharedScratchDir.toFile(), config.getCallbackURL(),
                    jobId.toString(), additionalBinds,
                    cancellationChecker, null, labels);
        }
        else {
            // Default is 7 days
            String timeout = System.getenv("DOCKER_JOB_TIMEOUT");
            new DockerRunner(config.getDockerURI()).run(
                    imageName, moduleName, inputFile.toFile(), token,
                    config.getLogger(), outputFile.toFile(), false, refDataDir,
                    sharedScratchDir.toFile(), config.getCallbackURL(),
                    jobId.toString(), additionalBinds, cancellationChecker, null, labels, null, cgroupParent, timeout);
        }
        return outputFile;
    }

}
