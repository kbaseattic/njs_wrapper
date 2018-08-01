package us.kbase.narrativejobservice.test;

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


public class ReaperTest {

    static AuthToken token;
    static String jobStatusURL;
    static Properties props;
    static Map<String, String> config;


    @Test
    public void testSimple() throws Exception {
        ReaperService r = new ReaperService();
        System.out.println(r.getIncompleteJobs());
        System.out.println(r.purgeGhostJobs());

    }

    @Test
    public void testWithAuthSimple() throws Exception {
        String host = config.get("ujs-mongodb-host");
        String db = config.get("ujs-mongodb-database");
        String user = config.get("ujs-mongodb-user");
        String pwd = config.get("ujs-mongodb-pwd");

        ReaperService r = new ReaperService(user, pwd, host, db);
        System.out.println(r.getIncompleteJobs());
        System.out.println(r.purgeGhostJobs());
    }

    private static void createMongoUserForMiniKB() throws Exception {
        String host = config.get("ujs-mongodb-host");
        String dbName = config.get("ujs-mongodb-database");
        String user = config.get("ujs-mongodb-user");
        String pwd = config.get("ujs-mongodb-pwd");

        MongoClient mongo = new MongoClient(host);
        DB db = mongo.getDB(dbName);
        db.addUser(user, pwd.toCharArray(), false);
    }

    @BeforeClass
    public static void setUpStuff() throws Exception {
        props = TesterUtils.props();
        token = TesterUtils.token(props);
        config = TesterUtils.loadConfig();
        createMongoUserForMiniKB();
    }

    @Test
    public void testNonRootUser() throws Exception {
        Process p = Runtime.getRuntime().exec("whoami");
        BufferedReader buffer = new BufferedReader(new InputStreamReader((p.getInputStream())));
        String userName = buffer.lines().collect(Collectors.joining("\n"));
        Assert.assertFalse("FAILURE: Do not run these tests as ROOT", userName.equals("root"));
    }
}