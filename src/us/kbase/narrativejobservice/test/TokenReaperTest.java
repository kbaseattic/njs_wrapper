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

        System.out.println(SDKLocalMethodRunner.miliSecondsToLive(token.getToken(),config));
    }



    @BeforeClass
    public static void setUpStuff() throws Exception {
        props = TesterUtils.props();
        token = TesterUtils.token(props);
        config = TesterUtils.loadConfig();

    }

}