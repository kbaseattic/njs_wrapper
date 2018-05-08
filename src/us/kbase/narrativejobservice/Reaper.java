package us.kbase.narrativejobservice;

import org.apache.commons.io.FileUtils;
import us.kbase.auth.AuthToken;
import us.kbase.common.utils.CondorUtils;

import java.io.File;
import java.net.URL;
import java.util.Map;

public class Reaper {

//    private static String checkCondor() throws Exception {
////        CondorUtils.condorQ("ABC", "ABC");
////        //Step 1, Get all jobs that are running or queued from UJS
////        //Step 2, Check to see if they have an entry in condor
////        //Step 3, if they do not have an entry in condor, mark them as ERROR
////       // return "\tSTATUS: 0 jobs found to mark as dead";
////        String tokenStr = "123";
////        final AuthToken tempToken = new AuthToken(tokenStr);
////        String jobSrvURL = "123";
////        final us.kbase.narrativejobservice.NarrativeJobServiceClient jobSrvClient =
////                new us.kbase.narrativejobservice.NarrativeJobServiceClient(new URL(jobSrvURL), tempToken);
////        jobSrvClient.setIsInsecureHttpConnectionAllowed(true);
////        return "OK";
//    }

    private static us.kbase.userandjobstate.UserAndJobStateClient getUjsClient(AuthToken auth,
                                                                               Map<String, String> config) throws Exception {
        String jobSrvUrl = config.get(us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        if (jobSrvUrl == null)
            throw new IllegalStateException("Parameter '" +
                    us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL +
                    "' is not defined in configuration");
        us.kbase.userandjobstate.UserAndJobStateClient ret = new us.kbase.userandjobstate.UserAndJobStateClient(new URL(jobSrvUrl), auth);
        ret.setIsInsecureHttpConnectionAllowed(true);
        ret.setAllSSLCertificatesTrusted(true);
        return ret;
    }

    static public void launchReaper() throws java.io.IOException, Exception {
        Thread reaper = new Thread(new Runnable() {
            @Override
            public void run() {
                File logFile = new File("/kb/deployment/logfile.txt");
                try {
                    FileUtils.touch(logFile);

                }
                catch (Exception e){
                    System.out.println("Couldn't create logfile");
                }
                while (true) {
                    try {
                        Thread.sleep(10000);

                        String msg1 = "REAPER STATUS: Checking to see if ghost jobs should be set to failed. ";
                        String msg2 = "MEMORY USAGE OF NJS: ";
                        String status = "checkCondorHere()";
                        ;
                        String memory = Long.toString(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                        try {
                            String string = String.format("%s\t%s\t%s\t%s\n", msg1, msg2, memory, status);
                            FileUtils.writeStringToFile(logFile, string, "UTF-8",true);

                        } catch (Exception E) {
                        }

                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (Thread.currentThread().isInterrupted())
                        break;
                }

            }
        });
        reaper.setDaemon(true);
        reaper.start();
    }
}
