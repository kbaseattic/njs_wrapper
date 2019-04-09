package us.kbase.narrativejobservice;

import com.mongodb.BulkWriteResult;
import org.apache.commons.io.FileUtils;
import org.ini4j.Ini;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


public class ReaperServiceServlet implements ServletContextListener {

    private Thread myThread = null;
    static Map<String, String> config;

    private ReaperService getReaperService() throws Exception {

        Ini config = new Ini(new File(System.getenv("KB_DEPLOYMENT_CONFIG")));
        String host = config.get("NarrativeJobService", "ujs-mongodb-host");
        String dbName = config.get("NarrativeJobService", "ujs-mongodb-database");
        String user = config.get("NarrativeJobService", "ujs-mongodb-user");
        String pwd = config.get("NarrativeJobService", "ujs-mongodb-pwd");
        return new ReaperService(user, pwd, host, dbName);
    }

    public void contextInitialized(ServletContextEvent sce) {

        if ((myThread == null) || (!myThread.isAlive())) {
            final File file = new File("reaper.log");
            final File error_file = new File("reaper.error");
            Thread myThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        ReaperService r = getReaperService();

                        while (true) {
                            String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + "\n";
                            FileUtils.writeStringToFile(file, "Running Job Reaper at " + time, true);
                            BulkWriteResult result = r.purgeGhostJobs();
                            if (result != null) {
                                FileUtils.writeStringToFile(file, result.toString() + " (" + time + ")\n", true);
                            } else {
                                FileUtils.writeStringToFile(file, "No Jobs To Purge." + " (" + time + ")\n", true);
                            }
                            //5 Minutes Before Each Run
                            Thread.sleep(1000 * 60 * 5);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            FileUtils.writeStringToFile(error_file, e.toString(), true);
                            FileUtils.writeStringToFile(error_file, e.getStackTrace().toString(), true);
                        } catch (Exception ignore) {
                        }
                    }
                }
            });
            myThread.isDaemon();
            myThread.start();
        } else {
            System.out.println("FAILED TO RUN REAPER");
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        try {
            myThread.interrupt();
        } catch (Exception ex) {
        }
    }
}
