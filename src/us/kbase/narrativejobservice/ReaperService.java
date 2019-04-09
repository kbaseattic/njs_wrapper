package us.kbase.narrativejobservice;


import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import com.mongodb.*;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.types.ObjectId;
import us.kbase.common.utils.CondorUtils;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class ReaperService {

    MongoClient mongoClient;
    DB db;
    DBCollection coll;


    final File reaperServiceLog = new File("reaperService.log");
    final File error_file = new File("reaperService.error");




    /**
     * Create a new instance of the reaper service and select the jobstate database on ci-mongo
     *
     * @throws Exception
     */
    public ReaperService() throws Exception {
        this.mongoClient = new MongoClient("ci-mongo:27017");
        this.db = this.mongoClient.getDB("userjobstate");
        this.coll = this.db.getCollection("jobstate");
    }

    public ReaperService(String userName, String password, String host, String database) throws Exception {
        //TODO Add support for replicate dbs
        List<MongoCredential> mc_list = new ArrayList<MongoCredential>();
        mc_list.add(MongoCredential.createMongoCRCredential(userName, database, password.toCharArray()));
        this.mongoClient = new MongoClient(new ServerAddress(host), mc_list);
        this.db = this.mongoClient.getDB("userjobstate");
        this.coll = this.db.getCollection("jobstate");
    }

    /**
     * Get a list of incomplete jobs from the UserJobState db.
     *
     * @return The complete list of UJS Job Ids that are marked as incomplete
     */
    public List<String> getIncompleteJobs() {

        final List<String> idList = new ArrayList<>();
        BasicDBObject query = new BasicDBObject("complete", new BasicDBObject("$ne", true));
        DBCursor cursor = coll.find(query);
        try {
            while (cursor.hasNext()) {
                idList.add(cursor.next().get("_id").toString());
            }
        } finally {
            cursor.close();
        }
        return idList;
    }

    /**
     * Get a list of jobs that are marked as incomplete in UJS, and are either
     * A) Not found in Condor or
     * B) Are found in HTCondor and are marked Removed or Submission_Err
     * jobStatus = {"Unexpanded": "0", "Idle": "1", "Running": "2",  "Removed": "3", "Completed": "4", "Held": "5", "Submission_Err": "6"}
     *
     * @return A list of ghost jobs to be removed
     * @throws Exception
     */
    public List<String> getGhostJobs() throws Exception {
        List<String> incompleteJobs = this.getIncompleteJobs();

        //Give condor a chance to catch up
        Thread.sleep(15000);
        String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + "\n";

        HashMap<String, String> idleRunningHeldJobs = CondorUtils.getIdleOrRunningOrHeldJobs();

        int run_count = 0;
        int idle_count = 0;
        int unknown = 0;
        int held_count = 0;

        int unexpanded_count = 0;

        for (String jobID : idleRunningHeldJobs.keySet()) {
            String status = idleRunningHeldJobs.get(jobID);
            if (status.equals("0"))
                unexpanded_count++;
            if (status.equals("1"))
                run_count++;
            else if (status.equals("2"))
                idle_count++;
            else if (status.equals("5"))
                held_count++;
            else
                unknown++;
        }

        List<String> deadJobs = new ArrayList<>();
        int dead = 0;
        int alive = 0;
        for (String jobID : incompleteJobs) {
            if (!idleRunningHeldJobs.containsKey(jobID)) {
                FileUtils.writeStringToFile(reaperServiceLog, jobID + " is dead" + " (" + time + ")\n", true);
                deadJobs.add(jobID);
                dead++;
            } else {
                alive++;
            }
        }
        String msg = String.format("Jobs: running=%d idle=%d held=%d unknown=%d unexpanded=%d alive=%d dead=%d", run_count, idle_count, held_count, unknown, unexpanded_count, alive, dead);
        FileUtils.writeStringToFile(reaperServiceLog, msg  + " (" + time + ")\n", true);

        String deadJobsList = String.join("\n", deadJobs);
        FileUtils.writeStringToFile(reaperServiceLog, deadJobsList  + " (" + time + ")\n", true);
        return deadJobs;
    }


    public static void notifySlack( List<String> ghostJobs) throws  Exception{

        try {
            String url = System.getenv("webhook_url");
            url =  "";
            URI slack = URI.create(url);
            String deadJobsList = String.join("\n", ghostJobs);

            //curl -X POST -H 'Content-type: application/json' --data "{'text':'${message}'}" $webhook_url

            //final Client CLI = ClientBuilder.newClient();
            //final ImmutableMap params = ImmutableMap.of("text", deadJobsList, "text2", deadJobsList);

            String payload = "data={" +
                    "\"text\": \"deadJobsList\", " +
                    "}";
            StringEntity entity = new StringEntity(payload,
                    ContentType.APPLICATION_FORM_URLENCODED);


            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost( url);
            request.setEntity(entity);
            HttpResponse response = httpClient.execute(request);
            System.out.println(response.getStatusLine().getStatusCode());


            BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder total = new StringBuilder();
            String line = null;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            r.close();
            System.out.println(total.toString());


//            final WebTarget wt = CLI.target(slack);
//            final Invocation.Builder req = wt.request();
//            Response res = req.post(Entity.json(params));
        }
        catch (Exception e){
            e.printStackTrace();
        }




    }

    /**
     * Get ghost jobs and then purge them by seting their status in UJS to
     * complete=true, error=true
     *
     * @return The result of the removed jobs, or Null
     * @throws Exception
     */
    public BulkWriteResult purgeGhostJobs() throws Exception {
        List<String> ghostJobs = getGhostJobs();
        BulkWriteResult result;
        notifySlack(ghostJobs);


        if(true) {
            return null;
        }

        if (ghostJobs.size() > 0) {
            BulkWriteOperation builder = coll.initializeOrderedBulkOperation();
            for (String jobID : ghostJobs) {

                BasicDBObject updateFields = new BasicDBObject();
                updateFields.append("complete", true);
                updateFields.append("error", true);
                BasicDBObject setQuery = new BasicDBObject();
                setQuery.append("$set", updateFields);
                builder.find(new BasicDBObject("_id", new ObjectId(jobID))).update(setQuery);
            }
            result = builder.execute();
            return result;
        } else {
            System.err.println("\nNo ghost jobs to purge. " + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
        }
        return null;
    }


    /**
     * Purge a list of jobs
     *
     * @param ghostJobs The Jobs To Purge
     * @param message   The Status to Update them With
     * @return
     * @throws Exception
     */
    public BulkWriteResult purgeListOfJobs(List<String> ghostJobs, String message) throws Exception {

        BulkWriteResult result;
        if (ghostJobs.size() > 0) {
            BulkWriteOperation builder = coll.initializeOrderedBulkOperation();
            for (String jobID : ghostJobs) {

                BasicDBObject updateFields = new BasicDBObject();
                updateFields.append("complete", true);
                updateFields.append("error", true);
                updateFields.append("status", message);
                updateFields.append("errormsg", message);
                BasicDBObject setQuery = new BasicDBObject();
                setQuery.append("$set", updateFields);
                builder.find(new BasicDBObject("_id", new ObjectId(jobID))).update(setQuery);
            }
            result = builder.execute();
            return result;
        } else {
            System.err.println("\nNo ghost jobs to purge. " + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
        }
        return null;
    }
}


