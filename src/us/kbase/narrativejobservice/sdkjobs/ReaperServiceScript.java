package us.kbase.narrativejobservice.sdkjobs;

import com.mongodb.BulkWriteResult;
import org.ini4j.Ini;
import us.kbase.narrativejobservice.ReaperService;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


//TODO Use an argument parsing library

/**********************************************************************************
 Usage
 Log into the NJS_WRAPPER Docker Container

 Run the following commands to see ghost jobs

 su kbase
 $NJSW_JAR = "/kb/deployment/lib/NJSWrapper-all.jar"
 java -cp $NJSW_JAR us.kbase.narrativejobservice.sdkjobs.ReaperServiceScript

 To Purge Jobs
 java -cp $NJSW_JAR us.kbase.narrativejobservice.sdkjobs.ReaperServiceScript -purge

 To Purge a list of jobs
 java -cp $NJSW_JAR us.kbase.narrativejobservice.sdkjobs.ReaperServiceScript <PATH_TO_FILE>

 <file>
 UJS_ID1
 UJS_ID2
 UJS_ID3
 </file>

 ***********************************************************************************/

public class ReaperServiceScript {


    public static ReaperService getReaperService() throws Exception {
        Ini config = new Ini(new File(System.getenv("KB_DEPLOYMENT_CONFIG")));
        String host = config.get("NarrativeJobService", "ujs-mongodb-host");
        String dbName = config.get("NarrativeJobService", "ujs-mongodb-database");
        String user = config.get("NarrativeJobService", "ujs-mongodb-user");
        String pwd = config.get("NarrativeJobService", "ujs-mongodb-pwd");
        return new ReaperService(user, pwd, host, dbName);
    }





    public static void main(String[] args) throws Exception {
        ReaperService rs = getReaperService();
        List<String> incompleteJobs = rs.getIncompleteJobs();
        List<String> ghostJobs = rs.getGhostJobs();


        System.out.println(String.format("Found %s incomplete jobs", incompleteJobs.size()));
        for (String ic : incompleteJobs) {
            //System.out.println(ic);
        }

        System.out.println(String.format("Found %s ghost jobs", ghostJobs.size()));
        for (String ghost : ghostJobs) {
            System.out.println(ghost);
        }

        if (args.length == 1 && args[0].equals("-slack")) {
            rs.notifySlack(ghostJobs);
        }
        else if (args.length == 1 && args[0].equals("-purge")) {
            BulkWriteResult r = rs.purgeGhostJobs();
            System.out.println(String.format("Modified %s entries in mongo", r.getModifiedCount()));
            System.out.println(r);
        }
        else if (args.length == 1 && ! args[0].equals("-purge")) {
            Path filePath = new File(args[0]).toPath();
            Charset charset = Charset.defaultCharset();
            List<String> listOfIds = Files.readAllLines(filePath, charset);
            System.out.println("About to mark as error");
            for(String file: listOfIds){
                System.out.println(file);
            }
            rs.purgeListOfJobs(listOfIds, "Job was manually cancelled by an administrator");
        }


    }


}

