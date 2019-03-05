import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import us.kbase.auth.AuthToken;
import us.kbase.narrativejobservice.sdkjobs.DockerRunner;
import us.kbase.narrativejobservice.test.TesterUtils;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;


public class TestAlpineDeleteRunner {


    static AuthToken token;
    static String jobStatusURL;
    static Properties props;
    static Map<String, String> config;


    @Test
    public void testAlpineCleaner() throws Exception {
        String baseDir = props.getProperty("condor.dir");
        String testDir = baseDir + "/" + "test_dir";
        String testFilePath = testDir + "/" + "test_file";

        Files.createDirectories(Paths.get(testDir));
        Files.createFile(Paths.get(testFilePath));

        URI dockerURI = new URI(config.get("awe.client.docker.uri"));
        new DockerRunner(dockerURI).runAlpineCleaner(new File(testDir));
        boolean fileExists = new File(testFilePath).exists();
        Assert.assertFalse(String.format("File %s still exists", testFilePath), fileExists);
    }

    @BeforeClass
    public static void setUpStuff() throws Exception {
        props = TesterUtils.props();
        config = TesterUtils.loadConfig();

    }

}