package us.kbase.narrativejobservice.test;

import java.io.File;

import junit.framework.Assert;

import us.kbase.common.taskqueue2.TaskQueueConfig;
import us.kbase.narrativejobservice.db.ExecEngineMongoDb;
import us.kbase.narrativejobservice.db.MigrationToMongo;

public class MigrationToMongoTester {
    public static void main(String[] args) throws Exception {
        TaskQueueConfig config = new TaskQueueConfig(1, new File("./temp_files/queuedb_ci"), null, null, 5, null);
        MongoDBHelper dbh = new MongoDBHelper("migrate2mongo", new File("temp_files"));
        try {
            dbh.startup(null);
            ExecEngineMongoDb db = new ExecEngineMongoDb("localhost:" + dbh.getMongoPort(), "exec_engine", null, null, null);
            Assert.assertTrue(MigrationToMongo.migrate(config, db, dbh.getWorkDir()));
            Assert.assertFalse(MigrationToMongo.migrate(config, db, dbh.getWorkDir()));
        } finally {
            dbh.shutdown();
        }
    }
}
