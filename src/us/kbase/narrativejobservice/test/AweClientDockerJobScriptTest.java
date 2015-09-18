package us.kbase.narrativejobservice.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.UObject;
import us.kbase.common.utils.TextUtils;
import us.kbase.narrativejobservice.AweClientDockerJobScript;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.narrativejobservice.RunJobParams;
import us.kbase.shock.client.BasicShockClient;
import us.kbase.shock.client.ShockNodeId;
import us.kbase.userandjobstate.UserAndJobStateClient;

public class AweClientDockerJobScriptTest {
    
    @Test
    public void mainTest() throws Exception {
        Properties props = props(new File("test.cfg"));
        String token = token(props);
        AuthToken authPart = new AuthToken(token);
        Map<String, String> config = new LinkedHashMap<String, String>();
        config.put(NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL, 
                "dockerhub-ci.kbase.us");
        config.put(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_SCRATCH, 
                "temp_files");
        config.put(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL, 
                "https://ci.kbase.us/services/userandjobstate/");
        config.put(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL, 
                "https://ci.kbase.us/services/shock-api");
        UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
        BasicShockClient shockClient = getShockClient(authPart, config);
        RunJobParams params = new RunJobParams().withMethod(
                "GenomeFeatureComparator.compare_genome_features")
                .withParams(Arrays.asList(UObject.fromJsonString(
                        "{\"genomeA\":\"myws.mygenome1\",\"genomeB\":\"myws.mygenome2\"}")));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        UObject.getMapper().writeValue(baos, params);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        String inputShockId = shockClient.addNode(bais, "job.json", "json").getId().getId();
        String ujsJobId = ujsClient.createJob();
        String outputShockId = shockClient.addNode().getId().getId();
        System.setProperty("KB_AUTH_TOKEN", token);
        AweClientDockerJobScript.main(new String[] {ujsJobId, inputShockId, outputShockId, 
                TextUtils.stringToHex(UObject.getMapper().writeValueAsString(config))});
        baos = new ByteArrayOutputStream();
        shockClient.getFile(new ShockNodeId(outputShockId), baos);
        baos.close();
        System.out.println(new String(baos.toByteArray()));
    }

    private static UserAndJobStateClient getUjsClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        String jobSrvUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        UserAndJobStateClient ret = new UserAndJobStateClient(new URL(jobSrvUrl), auth);
        ret.setIsInsecureHttpConnectionAllowed(true);
        ret.setAllSSLCertificatesTrusted(true);
        return ret;
    }

    private static BasicShockClient getShockClient(AuthToken auth, 
            Map<String, String> config) throws Exception {
        String shockUrl = config.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL);
        BasicShockClient ret = new BasicShockClient(new URL(shockUrl), auth);
        return ret;
    }

    private static String token(Properties props) throws Exception {
        return AuthService.login(get(props, "user"), get(props, "password")).getTokenString();
    }

    private static String get(Properties props, String propName) {
        String ret = props.getProperty(propName);
        if (ret == null)
            throw new IllegalStateException("Property is not defined: " + propName);
        return ret;
    }

    private static Properties props(File configFile)
            throws FileNotFoundException, IOException {
        Properties props = new Properties();
        InputStream is = new FileInputStream(configFile);
        props.load(is);
        is.close();
        return props;
    }
}
