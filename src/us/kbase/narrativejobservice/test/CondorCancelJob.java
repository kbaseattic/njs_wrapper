package us.kbase.narrativejobservice.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.ini4j.InvalidFileFormatException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import us.kbase.auth.AuthToken;
import us.kbase.catalog.*;
import us.kbase.catalog.ModuleInfo;
import us.kbase.common.service.*;
import us.kbase.common.test.TestException;
import us.kbase.common.test.controllers.mongo.MongoController;
import us.kbase.common.utils.AweUtils;
import us.kbase.common.utils.ProcessHelper;
import us.kbase.narrativejobservice.*;
import us.kbase.narrativejobservice.JobState;
import us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner;
import us.kbase.narrativejobservice.test.TesterUtils;
import us.kbase.userandjobstate.UserAndJobStateClient;
import us.kbase.workspace.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.URL;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


@SuppressWarnings("unchecked")
public class CondorCancelJob {
    private static AuthToken token = null;
    private static NarrativeJobServiceClient client = null;
    private static Server catalogWrapper = null;


    @Test
    public void testDeleteJob() throws Exception{
        System.out.println("Test [testDeleteJob]");
        String jobID = "5b4777d2e4b0d417818a2d59";
        System.out.println("ABOUT TO CANCEL: " + jobID);
        Thread.sleep(10000);
        client.cancelJob(new CancelJobParams().withJobId(jobID));
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties props = TesterUtils.props();
        token = TesterUtils.token(props);
        String njs_url = props.getProperty("njs_server_url");
        client = new NarrativeJobServiceClient(new URL(njs_url), token);
        client.setIsInsecureHttpConnectionAllowed(true);
    }

    public static void main(String[] args) throws Exception {
        beforeClass();
    }

}
