package us.kbase.narrativejobservice.test;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class DockerTest {

    public static DockerClient cl;
    public static String dockerURI = "unix:///var/run/docker.sock";


    public static DockerClient createDockerClient() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "ERROR");

        if (dockerURI != null) {
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(dockerURI).build();
            return DockerClientBuilder.getInstance(config).build();
        } else {
            return DockerClientBuilder.getInstance().build();
        }
    }


    @Test
    public void testNonRootUser() throws Exception {
        DockerClient cl = createDockerClient();
        cl.listImagesCmd().withShowAll(true).exec();
    }
}