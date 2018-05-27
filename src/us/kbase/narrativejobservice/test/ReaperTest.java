package us.kbase.narrativejobservice.test;

import org.ini4j.InvalidFileFormatException;
import org.junit.*;
import us.kbase.narrativejobservice.ReaperService;

import org.ini4j.InvalidFileFormatException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import us.kbase.auth.AuthToken;
import us.kbase.auth.ConfigurableAuthService;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;
import us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner;
import us.kbase.narrativejobservice.test.TesterUtils;
import us.kbase.workspace.CreateWorkspaceParams;
import us.kbase.workspace.WorkspaceClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import us.kbase.narrativejobservice.db.ExecEngineMongoDb;



public class ReaperTest {

    static AuthToken token;
    static String jobStatusURL;


//    @Test
//    public void testSimple() throws Exception {
//
//        ReaperPrototype r = new ReaperPrototype(token, jobStatusURL);
//        System.out.println("STATUS OF REPEAR=" + r.checkCondor());
//        assert(false);
//
//    }

    @Test
    public void testSimple() throws Exception {
        ReaperService r = new ReaperService();
        System.out.println(r.getIncompleteJobs());
        System.out.println(r.purgeGhostJobs());

    }



    @BeforeClass
    public static void setUpStuff() throws Exception {
        Properties props = TesterUtils.props();
        token = TesterUtils.token(props);
//        String njs_url = props.getProperty("njs_server_url");
        jobStatusURL = props.getProperty("jobstatus_url");
//        String config = TesterUtils.loadConfig();
//        setupWorkSpace();


    }


    @Test
    public void testNonRootUser() throws Exception {
        Process p = Runtime.getRuntime().exec("whoami");
        BufferedReader buffer = new BufferedReader(new InputStreamReader((p.getInputStream())));
        String userName = buffer.lines().collect(Collectors.joining("\n"));
        Assert.assertFalse("FAILURE: Do not run these tests as ROOT", userName.equals("root"));
    }


}