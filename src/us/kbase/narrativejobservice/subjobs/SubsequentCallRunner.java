package us.kbase.narrativejobservice.subjobs;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.ModuleVersionInfo;
import us.kbase.catalog.SelectModuleVersionParams;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.UObject;
import us.kbase.common.service.JsonServerServlet.RpcCallData;
import us.kbase.narrativejobservice.DockerRunner;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;

public class SubsequentCallRunner {
    private static final Set<String> asyncVersionTags = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList("dev", "beta", "release")));

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
    
    public SubsequentCallRunner(File mainJobDir, String methodName, 
            String serviceVer, int callbackPort, Map<String, String> config,
            DockerRunner.LineLogger logger) throws Exception {
        this.logger = logger;
        this.dockerURI = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI);
        String catalogUrl = config.get(NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        CatalogClient catClient = new CatalogClient(new URL(catalogUrl));
        catClient.setIsInsecureHttpConnectionAllowed(true);
        catClient.setAllSSLCertificatesTrusted(true);
        this.moduleName = methodName.substring(0, methodName.indexOf('.'));
        String imageVersion = serviceVer;
        ModuleVersionInfo mvi = null;
        if (imageVersion == null || asyncVersionTags.contains(imageVersion)) {
            ModuleInfo mi = catClient.getModuleInfo(new SelectOneModuleParams().withModuleName(moduleName));
            if (imageVersion == null) {
                mvi = mi.getRelease();
            } else if (imageVersion.equals("dev")) {
                mvi = mi.getDev();
            } else if (imageVersion.equals("beta")) {
                mvi = mi.getBeta();
            } else {
                mvi = mi.getRelease();
            }
            if (mvi == null)
                throw new IllegalStateException("Cannot extract " + imageVersion + " version for module: " + moduleName);
            imageVersion = mvi.getGitCommitHash();
        } else {
            try {
                mvi = catClient.getVersionInfo(new SelectModuleVersionParams()
                        .withModuleName(moduleName).withGitCommitHash(imageVersion));
            } catch (Exception ex) {
                throw new IllegalStateException("Error retrieving module version info about image " +
                        moduleName + " with version " + imageVersion, ex);
            }
        }
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
    
    public Map<String, Object> run(RpcCallData rpcCallData) throws Exception {
        File inputFile = new File(jobWorkDir, "input.json");
        File outputFile = new File(jobWorkDir, "output.json");
        UObject.getMapper().writeValue(inputFile, rpcCallData);
        System.out.println("dockerURI=" + dockerURI);
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
