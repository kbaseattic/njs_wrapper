package us.kbase.narrativejobservice;


import com.mongodb.*;
import org.bson.types.ObjectId;
import us.kbase.common.utils.CondorUtils;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class ReaperService {

    MongoClient mongoClient;
    DB db;
    DBCollection coll;


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

        HashMap<String, String> idleAndRunningJobs = CondorUtils.getIdleAndRunningJobs();

        int run_count = 0;
        int idle_count = 0;
        int unknown = 0;

        for (String jobID : idleAndRunningJobs.keySet()) {
            String status = idleAndRunningJobs.get(jobID);
            if (status.equals("1"))
                run_count += 1;
            else if (status.equals("2"))
                idle_count += 1;
            else
                unknown += 1;
        }

        List<String> deadJobs = new ArrayList<>();
        int dead = 0;
        int alive = 0;
        for (String jobID : incompleteJobs) {
            if (!idleAndRunningJobs.containsKey(jobID)) {
                System.out.println(jobID + " is dead");
                deadJobs.add(jobID);
                dead++;
            } else {
                System.out.println(jobID + " is still alive!");
                alive++;
            }
        }
        System.out.println(String.format("Num of jobs running=%d idle=%d unknown=%d alive=%d dead=%d", run_count, idle_count, unknown, alive, dead));
        return deadJobs;
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


