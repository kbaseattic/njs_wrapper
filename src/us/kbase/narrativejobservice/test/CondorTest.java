package us.kbase.narrativejobservice.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


import org.eclipse.jetty.server.Server;
import org.junit.*;
import us.kbase.common.service.UObject;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;


import us.kbase.auth.AuthToken;
import us.kbase.common.utils.CondorUtils;

import org.apache.commons.io.FileUtils;

import us.kbase.narrativejobservice.test.AweClientDockerJobScriptTest;
//import us.kbase.narrativejobservice.test.CatalogWrapper;

import us.kbase.common.service.JsonServerServlet;


import us.kbase.narrativejobservice.NarrativeJobServiceClient;
import us.kbase.narrativejobservice.RunJobParams;
import us.kbase.catalog.CatalogClient;

import org.ini4j.InvalidFileFormatException;
import us.kbase.common.service.JsonClientException;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.stream.*;

import java.util.HashMap;

public class CondorTest {



    @Test
    public void testFakeJobStatus() throws Exception{
        String ujsJobId = "5acd7057e4b057726bc40d7e";
        String state = (CondorUtils.getJobState(ujsJobId));
        System.out.println("State for" + ujsJobId  + " = " + state);
    }
    @Test
    public void testFakeJobPriority() throws Exception{
        String ujsJobId = "5acd7057e4b057726bc40d7e";
        String state = (CondorUtils.getJobPriority(ujsJobId));
        System.out.println("getJobPriority for" + ujsJobId  + " = " + state);
    }

//    @Test
//    public void testFakeJobSub() throws Exception{
//        String ujsJobId = "5acd7057e4b057726bc40d7e";
//        AuthToken token = new AuthToken("62IYPZGS7O773DBLZZCSE542BP4C2E7G");
//        String endpoint = "http://nginx/services/njs";
//        String basedir = "/njs_wrapper/newfolder";
//        String jobID = (CondorUtils.submitToCondorCLI(ujsJobId,token,"njs",endpoint,basedir));
//        System.out.println(jobID);
//    }
//
//



    //    @Test
//    public void testBogusJobStateAndPriority() throws Exception {
//        String jobId = "bogusJobId";
//        Assert.assertNull(CondorUtils.getJobState(jobId));
//        Assert.assertNull(CondorUtils.getJobPriority(jobId));
//        Assert.assertTrue(CondorUtils.checkJobIsNotDone(jobId));
//        Assert.assertFalse(CondorUtils.checkJobIsDoneWithoutError(jobId));
//    }
//
    @Test
    public void testNonRootUser() throws Exception {
        Process p = Runtime.getRuntime().exec("whoami");
        BufferedReader buffer = new BufferedReader(new InputStreamReader((p.getInputStream())));
        String userName = buffer.lines().collect(Collectors.joining("\n"));
        Assert.assertFalse("FAILURE: Do not run these tests as ROOT", userName.equals("root"));
    }
//
//    @Test
//    public void testSubmitJob() throws Exception {
//
//        String ujsJobId = "JobBatchNumber123";
//        String selfExternalUrl = null;
//        String submitFilePath = "test_data/condor_jobs/sample.sub";
//        String aweClientGroups = "SCHEDD";
//        float jobID = CondorUtils.submitToCondorCLI(ujsJobId, selfExternalUrl, submitFilePath, aweClientGroups);
//        String jobIDstring = Float.toString(jobID);
//        System.out.println("JOB ID RETURNED of the job is " + jobIDstring);
//        System.out.println("Position of the job is " + CondorUtils.getJobPriority(jobIDstring));
//        System.out.println("Status of the job is " + CondorUtils.getJobState(jobIDstring));
//        Thread.sleep(1000);
//        System.out.println("Status of the job is " + CondorUtils.getJobState(jobIDstring));
//        System.out.println("CHecking to see if job is NOT done ");
//        if (CondorUtils.checkJobIsNotDone(jobIDstring)) {
//            System.out.println("Job is NOT DONE");
//        } else {
//            System.out.println("Job is  DONE");
//        }
//        System.out.println("CHecking to see if job is done without error");
//        if (CondorUtils.checkJobIsDoneWithoutError(jobIDstring)) {
//            System.out.println("Job is done without error");
//        } else {
//            System.out.println("Job is done WITH ERROR");
//        }
//    }
//    //ci-mongo:27017
}