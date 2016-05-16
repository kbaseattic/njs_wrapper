package us.kbase.narrativejobservice.subjobs;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.type.TypeReference;

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
import us.kbase.narrativejobservice.NarrativeJobServiceServer;

public class SubsequentCallRunner {
    private static final Set<String> RELEASE_TAGS =
            AweClientDockerJobScript.RELEASE_TAGS;
    private static final String RELEASE = AweClientDockerJobScript.RELEASE;
    private static final String DEV = AweClientDockerJobScript.DEV;

    private String jobId;
    private String moduleName;
    private File sharedScratchDir;
    private File jobDir;
    private File jobWorkDir;
    private String imageName;
    private String callbackUrl;
    private DockerRunner.LineLogger logger;
    private String dockerURI;
    private String token;
    
    private final ModuleRunVersion mrv;
    
    public SubsequentCallRunner(File mainJobDir, ModuleMethod modmeth, 
            String serviceVer, int callbackPort, Map<String, String> config,
            DockerRunner.LineLogger logger) throws IOException,
            JsonClientException {
        this.logger = logger;
        this.dockerURI = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI);
        String catalogUrl = config.get(NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        CatalogClient catClient = new CatalogClient(new URL(catalogUrl));
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
        File subjobsDir = new File(mainJobDir, "subjobs");
        if (!subjobsDir.exists())
            subjobsDir.mkdirs();
        long pref = System.currentTimeMillis();
        String suff = imageName.replace(':', '_').replace('/', '_');
        for (;;pref++) {
            jobId = pref + "_" + suff;
            this.jobDir = new File(subjobsDir, jobId);
            if (!jobDir.exists())
                break;
        }
        this.jobDir.mkdirs();
        this.jobWorkDir = new File(jobDir, "workdir");
        this.jobWorkDir.mkdirs();
        this.callbackUrl = CallbackServer.getCallbackUrl(callbackPort);
        File srcTokenFile = new File(srcWorkDir, "token");
        this.token = FileUtils.readFileToString(srcTokenFile);
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
    
    public Map<String, Object> run(RpcCallData rpcCallData) throws Exception {
        File inputFile = new File(jobWorkDir, "input.json");
        File outputFile = new File(jobWorkDir, "output.json");
        UObject.getMapper().writeValue(inputFile, rpcCallData);
        System.out.println("dockerURI=" + dockerURI);
        logger.logNextLine("Running docker container for image: " + imageName, false);
        new DockerRunner(dockerURI).run(imageName, moduleName, inputFile, token, logger, outputFile, false, 
                null, sharedScratchDir, callbackUrl);
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
