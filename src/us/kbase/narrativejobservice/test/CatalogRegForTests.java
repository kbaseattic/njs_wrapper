package us.kbase.narrativejobservice.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import us.kbase.auth.AuthToken;
import us.kbase.catalog.BuildLog;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.GetBuildLogParams;
import us.kbase.catalog.RegisterRepoParams;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;

public class CatalogRegForTests {
    private static AuthToken token;
    private static CatalogClient catalogClient;
    
    public static void main(String[] args) throws Exception {
        String catalogUrl = TesterUtils.loadConfig().get(
                NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        token = TesterUtils.token(TesterUtils.props());
        catalogClient = new CatalogClient(new URL(catalogUrl), token);
        makeMyselfDeveloper();
        registerModule("https://github.com/kbaseIncubator/onerepotest");
        registerModule("https://github.com/kbasetest/njs_sdk_test_1");
        registerModule("https://github.com/kbasetest/njs_sdk_test_2");
        registerModule("https://github.com/kbasetest/njs_sdk_test_3");
        promoteModuleToBeta("njs_sdk_test_2");
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
    
    private static void promoteModuleToBeta(String moduleName) throws Exception {
        catalogClient.pushDevToBeta(new SelectOneModuleParams().withModuleName(moduleName));
    }
}
