//package us.kbase.narrativejobservice.sdkjobs;
//
//import com.mongodb.DB;
//import com.mongodb.DBCollection;
//import com.mongodb.MongoClient;
//import org.ini4j.Ini;
//import us.kbase.auth.AuthToken;
//import us.kbase.common.executionengine.LineLogger;
//import us.kbase.narrativejobservice.LogLine;
//import us.kbase.narrativejobservice.NarrativeJobServiceClient;
//import us.kbase.narrativejobservice.ReaperService;
//import us.kbase.narrativejobservice.sdkjobs.ReaperServiceScript;
//import us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner;
//import us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//
//public class RemoveJobScript {
//
//
//    private static synchronized void flushLog(NarrativeJobServiceClient jobSrvClient,
//                                              String jobId, List<LogLine> logLines) {
//        System.out.println("ABOUT TO LOG IT!");
//
//        if (logLines.isEmpty())
//            return;
//        try {
//            System.out.println("ABOUT TO LOG IT!");
//
//            logLines.clear();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    private static synchronized void addLogLine(NarrativeJobServiceClient jobSrvClient,
//                                                String jobId, List<LogLine> logLines, LogLine line) {
//        logLines.add(line);
//        if (line.getIsError() != null && line.getIsError() == 1L) {
//            System.err.println(line.getLine());
//        } else {
//            System.out.println(line.getLine());
//        }
//    }
//
//
//    public static void logSomething() throws Exception {
////        Ini config = new Ini(new File(System.getenv("KB_DEPLOYMENT_CONFIG")));
////        String host = config.get("NarrativeJobService", "kbase.endpoint");
////        String token = config.get("NarrativeJobService", "awe.readonly.admin.token");
//
//
//        AuthToken tempToken = new AuthToken("23TPMFA7WBRJZV5FQNVRRFJHHOX54GC7", "<unknown>");
//
//
//        String host = "https://ci.kbase.us/services/njs_wrapper";
//        NarrativeJobServiceClient jobSrvClient = SDKLocalMethodRunner.getJobClient(host, tempToken);
//        String jobId = "5c09930ce4b0028b4add7010";
//        final List<LogLine> logLines = new ArrayList<LogLine>();
//        logLines.add(new LogLine().withLine("TEST LOG").withIsError(1L));
//        jobSrvClient.addJobLogs(jobId, logLines);
//
//    }
//
//
//
////
////
////        final LineLogger log = new LineLogger() {
////            @Override
////            public void logNextLine(String line, boolean isError) {
////                addLogLine(jobSrvClient, jobId, logLines,
////                        new LogLine().withLine(line)
////                                .withIsError(isError ? 1L : 0L));
////            }
////        };
////
////        log.logNextLine("HEY THIS IS A TEST LOG", true);
////
////        flushLog(jobSrvClient, jobId, logLines);
////    }
//
//    public static void main(String[] args) throws Exception{
//        logSomething();
//    }
//
//
////    MongoClient mongoClient;
////    DB db;
////    DBCollection coll;
//
////    public void abc() throws Exception {
////
//////        final NarrativeJobServiceClient jobSrvClient = new SDKMethodRunner.get;
//////
//////
//////        SDKLocalMethodRunner.getJobClient();
//////
//////        ReaperService rs = ReaperServiceScript.getReaperService();
//////
//////        List<String> listOfJobs = null;
//////        rs.purgeListOfJobs(listOfJobs, "Job was cancelled due to an administrator");
//////
//////        Use
//////
//////                JobSr
////
////
////    }
//
//
//
//}
//
