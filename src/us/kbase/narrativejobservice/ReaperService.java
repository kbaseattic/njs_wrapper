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
        BasicDBObject query = new BasicDBObject("complete", false);
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
        Thread.sleep(30000);

        HashMap<String, String> runningCondorJobs = CondorUtils.getAllJobStates();

        List<String> deadJobs = new ArrayList<>();
        for (String jobID : incompleteJobs) {
            if (runningCondorJobs.containsKey(jobID)) {
                String status = runningCondorJobs.get(jobID);
                if (status == "3" || status == "6")
                    deadJobs.add(jobID);
            } else {
                deadJobs.add(jobID);
            }
        }
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
        } else {
            System.err.println("No ghost jobs to purge. " + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
        }
        return null;
    }
}


