package us.kbase.narrativejobservice.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import us.kbase.auth.AuthToken;
import us.kbase.catalog.BuildLog;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.GetBuildLogParams;
import us.kbase.catalog.RegisterRepoParams;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.UObject;
import us.kbase.narrativejobservice.CheckJobsParams;
import us.kbase.narrativejobservice.CheckJobsResults;
import us.kbase.narrativejobservice.GetJobLogsParams;
import us.kbase.narrativejobservice.JobState;
import us.kbase.narrativejobservice.JsonRpcError;
import us.kbase.narrativejobservice.LogLine;
import us.kbase.narrativejobservice.NarrativeJobServiceClient;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.narrativejobservice.RunJobParams;

public class CatalogRegForTests {
    private static AuthToken token;
    private static CatalogClient catalogClient;
    
    public static void main(String[] args) throws Exception {
        String catalogUrl = TesterUtils.loadConfig().get(
                NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        token = TesterUtils.token(TesterUtils.props());
        catalogClient = new CatalogClient(new URL(catalogUrl), token);
        //makeMyselfDeveloper();
        //registerModule("https://github.com/kbaseIncubator/onerepotest");
        //registerModule("https://github.com/kbasetest/njs_sdk_test_1");
        //registerModule("https://github.com/kbasetest/njs_sdk_test_2");
        //registerModule("https://github.com/kbasetest/njs_sdk_test_3");
        //promoteModuleToBeta("njs_sdk_test_2");
        submitTestJob();
    }

    private static void makeMyselfDeveloper() throws Exception {
        // It would work only if test user was added to catalog admins
        catalogClient.approveDeveloper(token.getUserName());
    }
    
    private static void registerModule(String gitUrl) throws Exception {
        String regId = catalogClient.registerRepo(new RegisterRepoParams().withGitUrl(gitUrl));
        System.out.println("Registration ID for [" + gitUrl + "]: " + regId);
        List<String> logLines = new ArrayList<String>();
        System.out.println("Logs:");
        while (true) {
            BuildLog log = catalogClient.getParsedBuildLog(
                    new GetBuildLogParams().withRegistrationId(regId));
            for (int i = logLines.size(); i < log.getLog().size(); i++) {
                String line = log.getLog().get(i).getContent().trim();
                logLines.add(line);
                System.out.println("[" + (i + 1) + "] " + line);
            }
            String state = log.getRegistration();
            if (state != null && (state.equals("error") || state.equals("complete"))) {
                break;
            }
            Thread.sleep(1000);
        }
    }
    
    private static void submitTestJob() throws Exception {
        String moduleName = "onerepotest";
        String methodName = "send_data";
        String serviceVer = "dev";
        RunJobParams params = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer).withAppId("myapp/foo")
                .withParams(Arrays.asList(UObject.fromJsonString(
                        "{\"genomeA\":\"myws.mygenome1\",\"genomeB\":\"myws.mygenome2\"}")));
        String njsUrl = TesterUtils.loadConfig().get(
                NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL);
        NarrativeJobServiceClient njsClient = new NarrativeJobServiceClient(new URL(njsUrl), token);
        String jobId = njsClient.runJob(params);
        System.out.println("Job with ID=" + jobId + " was scheduled for App " + 
                moduleName + "." + methodName);
        JobState ret = null;
        for (int i = 0; i < 60; i++) {
            try {
                CheckJobsResults retAll = njsClient.checkJobs(new CheckJobsParams().withJobIds(
                        Arrays.asList(jobId)).withWithJobParams(1L));
                ret = retAll.getJobStates().get(jobId);
                if (ret == null) {
                    JsonRpcError error = retAll.getCheckError().get(jobId);
                    System.out.println("Error: " + error);
                    break;
                }
                System.out.println("Job finished: " + ret.getFinished());
                if (ret.getFinished() != null && ret.getFinished() == 1L) {
                    break;
                }
                Thread.sleep(5000);
            } catch (ServerException ex) {
                System.out.println(ex.getData());
                throw ex;
            }
        }
        System.out.println("Job state: " + UObject.getMapper().writeValueAsString(ret));
        System.out.println("------------------------------------------------");
        System.out.println("Logs:");
        List<LogLine> lines = njsClient.getJobLogs(new GetJobLogsParams().withJobId(jobId)
                .withSkipLines((long)0)).getLines();
        for (LogLine line : lines) {
            String lineText = line.getLine();
            System.out.println("LOG: " + lineText);
        }
    }
    
    private static void promoteModuleToBeta(String moduleName) throws Exception {
        catalogClient.pushDevToBeta(new SelectOneModuleParams().withModuleName(moduleName));
    }
}
