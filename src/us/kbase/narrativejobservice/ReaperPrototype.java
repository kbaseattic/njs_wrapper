package us.kbase.narrativejobservice;

import org.apache.commons.io.FileUtils;
import us.kbase.auth.AuthToken;
import us.kbase.common.utils.CondorUtils;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import us.kbase.userandjobstate.ListJobsParams;


public class ReaperPrototype {

    private AuthToken token;
    private String ujsURL = "123";


    private void getUJSJobs(){
        //Query UJS
        //QueuedJobs = ujs.getJobs(Queued and within 2 weeks)
        //RunningJobs = ujs.getJobs(Started and within 2 weeks)
        //Condor_Queued = Get QueuedJobs
        //Condor_Running = Get RunningJobs

        //Foreach queued_job
        // if job exists in condor, OK!
        // Else, mark job as failed in UJS

        //Foreach Running job
        //If job exists in condor, OK
        //Else mark job as failed in UJS
    }


    public ReaperPrototype(AuthToken token, String ujsURL){
        this.token = token;
        this.ujsURL = ujsURL;
    }

    private us.kbase.userandjobstate.UserAndJobStateClient getUjsClient() throws Exception {
        String jobSrvUrl = ujsURL;
        us.kbase.userandjobstate.UserAndJobStateClient ret = new us.kbase.userandjobstate.UserAndJobStateClient(new URL(ujsURL), this.token);
        ret.setIsInsecureHttpConnectionAllowed(true);
        ret.setAllSSLCertificatesTrusted(true);
        return ret;
    }


    public String checkCondor() throws Exception {
        us.kbase.userandjobstate.UserAndJobStateClient cli = getUjsClient();
        List<String> services = new ArrayList<String>();
        services.add("ujs");


       // System.out.println(cli.listJobs(new ListJobsParams()));
        System.out.println(cli.listJobs(services,""));
        return "OK";
    }


}
