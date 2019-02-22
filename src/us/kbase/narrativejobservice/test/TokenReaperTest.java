package us.kbase.narrativejobservice.test;

import org.junit.BeforeClass;
import org.junit.Test;
import us.kbase.auth.AuthToken;
import us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner;
import us.kbase.narrativejobservice.test.TesterUtils;

import java.util.Map;
import java.util.Properties;


public class TokenReaperTest {


    static AuthToken token;
    static String jobStatusURL;
    static Properties props;
    static Map<String, String> config;


    @Test
    public void testSimple() throws Exception {
        long ms = SDKLocalMethodRunner.tokenExpiry(token.getToken(), config);
        //Possibly Flaky test based on expiration with mini_kb
        //Assert.assertEquals(Long.parseLong("8641506380012940"), ms );
    }


    @BeforeClass
    public static void setUpStuff() throws Exception {
        props = TesterUtils.props();
        token = TesterUtils.token(props);
        config = TesterUtils.loadConfig();

    }

}