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


    DBCollection ujs_jobstate_coll;
    DBCollection njs_exec_engine_coll;


    /**
     * Create a new instance of the reaper service and select the jobstate database on ci-mongo
     *
     * @throws Exception
     */
    public ReaperService() throws Exception {
        MongoClient mongoClient = new MongoClient("ci-mongo:27017");
        this.ujs_jobstate_coll = mongoClient.getDB("userjobstate").getCollection("jobstate");
        this.njs_exec_engine_coll = mongoClient.getDB("exec_engine").getCollection("exec_tasks");
    }

    public ReaperService(HashMap<String, String> njs_config, HashMap<String, String> ujs_config) throws Exception {
        //TODO Add support for replicate dbs
        List<MongoCredential> njs_mcList = new ArrayList<MongoCredential>();
        njs_mcList.add(MongoCredential.createMongoCRCredential(njs_config.get("user"), njs_config.get("dbName"), njs_config.get("pwd").toCharArray()));
        MongoClient njsMongoClient = new MongoClient(new ServerAddress(njs_config.get("host")), njs_mcList);
        this.njs_exec_engine_coll = njsMongoClient.getDB(njs_config.get("dbName")).getCollection("exec_tasks");

        List<MongoCredential> ujs_mcList = new ArrayList<MongoCredential>();
        ujs_mcList.add(MongoCredential.createMongoCRCredential(ujs_config.get("user"), ujs_config.get("dbName"), ujs_config.get("pwd").toCharArray()));
        MongoClient ujsMongoClient = new MongoClient(new ServerAddress(ujs_config.get("host")), ujs_mcList);
        this.ujs_jobstate_coll = ujsMongoClient.getDB(ujs_config.get("dbName")).getCollection("jobstate");
    }


    /**
     * Get a list of incomplete jobs from the UserJobState db.
     *
     * @return The complete list of UJS Job Ids that are marked as incomplete
     */
    public List<String> getIncompleteJobs() {

        final List<String> idList = new ArrayList<>();
        BasicDBObject query = new BasicDBObject("complete", false);
        DBCursor cursor = this.ujs_jobstate_coll.find(query);
        try {
            while (cursor.hasNext()) {
                idList.add(cursor.next().get("_id").toString());
            }
        } finally {
            cursor.close();
        }

        //MIGHT NEED TO BATCH THIS INTO SMALLER QUERIES of 1000 ids
        DBObject scheduler_query = QueryBuilder.start("ujs_job_id").in(idList).and(new BasicDBObject("scheduler_type", "condor")).get();
        BasicDBObject fields = new BasicDBObject();
        fields.put("ujs_job_id", 1);
        fields.put("scheduler_type", 1);

        final List<String> idListWithCondorJobs = new ArrayList<>();
        DBCursor njs_cursor = this.njs_exec_engine_coll.find(scheduler_query, fields);


        try {
            while (njs_cursor.hasNext()) {
                idListWithCondorJobs.add(njs_cursor.next().get("ujs_job_id").toString());
            }
        } finally {
            njs_cursor.close();
        }

        return idListWithCondorJobs;
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
            BulkWriteOperation builder = ujs_jobstate_coll.initializeOrderedBulkOperation();
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


