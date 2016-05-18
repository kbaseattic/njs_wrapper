package us.kbase.narrativejobservice.subjobs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenFormatException;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.ModuleVersionInfo;
import us.kbase.catalog.SelectModuleVersionParams;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.UObject;
import us.kbase.common.service.JsonServerServlet.RpcCallData;
import us.kbase.common.utils.ModuleMethod;
import us.kbase.narrativejobservice.AweClientDockerJobScript;
import us.kbase.narrativejobservice.DockerRunner;

public class SubsequentCallRunner {
    private static final Set<String> RELEASE_TAGS =
            AweClientDockerJobScript.RELEASE_TAGS;
    private static final String RELEASE = AweClientDockerJobScript.RELEASE;
    private static final String DEV = AweClientDockerJobScript.DEV;

    private final AuthToken token;
    private final String moduleName;
    private final File sharedScratchDir;
    private final File jobWorkDir;
    private final String imageName;
    private final URL callbackUrl;
    private final DockerRunner.LineLogger logger;
    private final URI dockerURI;
    private final ModuleRunVersion mrv;
    
    public SubsequentCallRunner(
            final AuthToken token,
            final UUID jobId,
            final File mainJobDir,
            final ModuleMethod modmeth, 
            String serviceVer,
            final URL callbackURL,
            final URI dockerURI,
            final URL catalogURL,
            final DockerRunner.LineLogger logger)
            throws IOException, JsonClientException,
            TokenFormatException {
        this.token = token;
        this.logger = logger;
        this.dockerURI = dockerURI;
        this.callbackUrl = callbackURL;
        CatalogClient catClient = new CatalogClient(catalogURL);
        catClient.setIsInsecureHttpConnectionAllowed(true);
        catClient.setAllSSLCertificatesTrusted(true);
        //TODO code duplicated in AweClientDockerJobScript
        this.moduleName = modmeth.getModule();
        final ModuleInfo mi;
        try {
            mi = catClient.getModuleInfo(
                new SelectOneModuleParams().withModuleName(moduleName));
        } catch (ServerException se) {
            throw new IllegalArgumentException(String.format(
                    "Error looking up module %s: %s", moduleName,
                    se.getLocalizedMessage()));
        }
        final ModuleVersionInfo mvi;
        if (serviceVer == null || RELEASE_TAGS.contains(serviceVer)) {
            if (serviceVer == null || serviceVer == RELEASE) {
                mvi = mi.getRelease();
                serviceVer = RELEASE;
            } else if (serviceVer.equals(DEV)) {
                mvi = mi.getDev();
            } else {
                mvi = mi.getBeta();
            }
            if (mvi == null) {
                // the requested release does not exist
                throw new IllegalArgumentException(String.format(
                        "There is no release version '%s' for module %s",
                        serviceVer, moduleName));
            }
        } else {
            try {
                mvi = catClient.getVersionInfo(new SelectModuleVersionParams()
                        .withModuleName(moduleName)
                        .withGitCommitHash(serviceVer));
                serviceVer = null;
            } catch (ServerException se) {
                throw new IllegalArgumentException(String.format(
                        "Error looking up module %s with version %s: %s",
                        moduleName, serviceVer, se.getLocalizedMessage()));
            }
        }
        mrv = new ModuleRunVersion(
                new URL(mi.getGitUrl()), modmeth,
                mvi.getGitCommitHash(), mvi.getVersion(), serviceVer);
        imageName = mvi.getDockerImgName();
        File srcWorkDir = new File(mainJobDir, "workdir");
        this.sharedScratchDir = new File(srcWorkDir, "tmp");
        if (!sharedScratchDir.exists())
            sharedScratchDir.mkdirs();
        File subjobsDir = new File(mainJobDir, "subjobs");
        if (!subjobsDir.exists())
            subjobsDir.mkdirs();
        final String suff = imageName.replace(':', '_').replace('/', '_');
        final String workdir = jobId.toString() +  "_" + suff;
        final File jobDir = new File(subjobsDir, workdir);
        jobDir.mkdirs();
        this.jobWorkDir = new File(jobDir, "workdir");
        this.jobWorkDir.mkdirs();
        File srcConfigPropsFile = new File(srcWorkDir, "config.properties");
        File dstConfigPropsFile = new File(jobWorkDir, "config.properties");
        FileUtils.copyFile(srcConfigPropsFile, dstConfigPropsFile);
    }
    
    /**
     * @return the version information for the module to be run.
     */
    public ModuleRunVersion getModuleRunVersion() {
        return mrv;
    }
    
    public Map<String, Object> run(RpcCallData rpcCallData)
            throws IOException, InterruptedException {
        File inputFile = new File(jobWorkDir, "input.json");
        File outputFile = new File(jobWorkDir, "output.json");
        //TODO NOW sometimes after starting the server for the first time 
        // (directly from the CallbackServer main method) this will throw NPE
        System.out.println("Params: " + rpcCallData.getParams());
        UObject.getMapper().writeValue(inputFile, rpcCallData);
        System.out.println("dockerURI=" + dockerURI);
        logger.logNextLine("Running docker container for image: " + imageName, false);
        new DockerRunner(dockerURI).run(
                imageName, moduleName, inputFile, token, logger, outputFile,
                false, null, sharedScratchDir, callbackUrl);
        if (outputFile.exists()) {
            return UObject.getMapper().readValue(outputFile, new TypeReference<Map<String, Object>>() {});
        } else {
            String errorMessage = "Unknown server error (output data wasn't produced)";
            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("name", "JSONRPCError");
            error.put("code", -32601);
            error.put("message", errorMessage);
            error.put("error", errorMessage);
            Map<String, Object> jsonRpcResponse = new LinkedHashMap<String, Object>();
            jsonRpcResponse.put("version", "1.1");
            jsonRpcResponse.put("error", error);
            return jsonRpcResponse;
        }
    }
}
