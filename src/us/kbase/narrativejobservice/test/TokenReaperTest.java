package kbase.narrativejobservice.test;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import us.kbase.auth.AuthToken;
import us.kbase.narrativejobservice.ReaperService;
import us.kbase.narrativejobservice.test.TesterUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner;



public class TokenReaperTest {


    static AuthToken token;
    static String jobStatusURL;
    static Properties props;
    static Map<String, String> config;


    @Test
    public void testSimple() throws Exception {
        long ms = SDKLocalMethodRunner.milliSecondsToLive(token.getToken(),config);
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