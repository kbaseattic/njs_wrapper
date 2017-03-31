package us.kbase.narrativejobservice.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import us.kbase.auth.AuthToken;
import us.kbase.catalog.BuildLog;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.GetBuildLogParams;
import us.kbase.catalog.RegisterRepoParams;
import us.kbase.catalog.ReleaseReview;
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
import us.kbase.workspace.CreateWorkspaceParams;
import us.kbase.workspace.GetModuleInfoParams;
import us.kbase.workspace.ModuleInfo;
import us.kbase.workspace.RegisterTypespecParams;
import us.kbase.workspace.WorkspaceClient;

public class CatalogRegForTests {
    private static AuthToken token;
    private static CatalogClient catalogClient;
    private static NarrativeJobServiceClient njsClient;
    private static WorkspaceClient wsCl;
    
    public static void main(String[] args) throws Exception {
        String catalogUrl = TesterUtils.loadConfig().get(
                NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        token = TesterUtils.token(TesterUtils.props());
        catalogClient = new CatalogClient(new URL(catalogUrl), token);
        String njsUrl = TesterUtils.loadConfig().get(
                NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL);
        njsClient = new NarrativeJobServiceClient(new URL(njsUrl), token);
        String wsUrl = TesterUtils.loadConfig().get(
                NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
        wsCl = new WorkspaceClient(new URL(wsUrl), token);
        //
        //makeUserDeveloper("someone");  // token.getUserName()
        //registerModule("https://github.com/kbaseIncubator/onerepotest");
        //registerModule("https://github.com/kbasetest/njs_sdk_test_1");
        //registerModule("https://github.com/kbasetest/njs_sdk_test_2");
        //registerModule("https://github.com/kbasetest/njs_sdk_test_3");
        //promoteModuleToBetaRelease("njs_sdk_test_2", true, false);
        //submitTestJob();
        //registerModule("https://github.com/kbaseapps/DataPaletteService");
        //startDynamicService("DataPaletteService");
        //registerModule("https://github.com/kbaseapps/DataFileUtil");
        //registerModule("https://github.com/kbaseapps/AssemblyUtil");
        //registerModule("https://github.com/kbaseapps/SetAPI");
        //registerModule("https://github.com/kbaseapps/NarrativeService");
        //registerModule("https://github.com/kbaseapps/kb_ea_utils");
        //promoteModuleToBetaRelease("kb_ea_utils", true, true);
        //registerModule("https://github.com/kbaseapps/ReadsUtils");
        //submitAssemblyJob();
        //promoteModuleToBetaRelease("GenomeFileUtil", true, true);
        //migrateWorkspaceTypesToNext("KBaseReport");
    }

    private static void makeUserDeveloper(String userName) throws Exception {
        // It would work only if test user was added to catalog admins
        catalogClient.approveDeveloper(userName);
    }
    
    private static void registerModule(String gitUrl) throws Exception {
        registerModule(gitUrl, null);
    }
    
    private static void registerModule(String gitUrl, String commit) throws Exception {
        String regId = catalogClient.registerRepo(new RegisterRepoParams().withGitUrl(gitUrl)
                .withGitCommitHash(commit));
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
        String jobId = njsClient.runJob(params);
        System.out.println("Job with ID=" + jobId + " was scheduled for App " + 
                moduleName + "." + methodName);
        showJobLogs(jobId);
    }
    
    private static void showJobLogs(String jobId) throws Exception {
        JobState ret = null;
        boolean started = true;
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
                System.out.println("Job finished: " + ret.getFinished() + ", state=" + 
                        UObject.getMapper().writeValueAsString(ret));
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
    
    private static void submitAssemblyJob() throws Exception {
        String testWorkspace = "test_workspace";
        try {
            wsCl.createWorkspace(new CreateWorkspaceParams().withWorkspace(testWorkspace));
        } catch (Exception ex) {
            System.out.println("Error creating workspace: " + ex.getMessage());
        }
        String moduleName = "AssemblyUtil";
        String methodName = "save_assembly_from_fasta";
        String serviceVer = "dev";
        RunJobParams params = new RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer).withAppId("myapp/foo")
                .withParams(Arrays.asList(UObject.fromJsonString(
                        "{\"workspace_name\":\"" + testWorkspace + "\"," +
                        "\"assembly_name\":\"Assembly.1\"," +
                        "\"ftp_url\":\"ftp://ftp.ncbi.nlm.nih.gov/genomes/genbank/bacteria/Escherichia_coli/reference/GCA_000005845.2_ASM584v2/GCA_000005845.2_ASM584v2_genomic.fna.gz\"}")));
        String jobId = njsClient.runJob(params);
        System.out.println("Job with ID=" + jobId + " was scheduled for App " + 
                moduleName + "." + methodName);
        showJobLogs(jobId);
    }
    
    private static void showWorkspaceTypes(String moduleName) throws Exception {
        ModuleInfo mi = wsCl.getModuleInfo(new GetModuleInfoParams().withMod(moduleName));
        System.out.println(mi);
        System.out.println("\nDate: " + new Date(mi.getVer()));
        System.out.println(wsCl.getAllTypeInfo(moduleName));
    }

    private static void promoteModuleToBetaRelease(String moduleName, boolean beta, boolean release) throws Exception {
        if (beta) {
            catalogClient.pushDevToBeta(new SelectOneModuleParams().withModuleName(moduleName));
        }
        if (release) {
            catalogClient.requestRelease(new SelectOneModuleParams().withModuleName(moduleName));
            catalogClient.reviewReleaseRequest(new ReleaseReview().withModuleName(moduleName).withDecision("approved"));
        }
    }

    private static void migrateWorkspaceTypesToNext(String moduleName, String... types) throws Exception {
        ModuleInfo orig = wsCl.getModuleInfo(new GetModuleInfoParams().withMod(moduleName));
        String spec = orig.getSpec();
        List<String> newTypes = new ArrayList<String>(Arrays.asList(types));
        System.out.println("Adding types: " + newTypes);
        WorkspaceClient wsNext = new WorkspaceClient(
                new URL("https://next.kbase.us/services/ws"), "<next-user>", "<next-password>");
        wsNext.setIsInsecureHttpConnectionAllowed(true);
        wsNext.setAllSSLCertificatesTrusted(true);
        System.out.println("Registered types: " + wsNext.registerTypespec(
                new RegisterTypespecParams().withSpec(spec)
                .withDryrun(0L).withNewTypes(newTypes)));
        wsNext.releaseModule(moduleName);
        ModuleInfo mi = wsNext.getModuleInfo(new GetModuleInfoParams().withMod(moduleName));
        System.out.println(mi);
        System.out.println("\nDate: " + new Date(mi.getVer()));
        System.out.println(wsNext.getAllTypeInfo(moduleName));
    }
}
