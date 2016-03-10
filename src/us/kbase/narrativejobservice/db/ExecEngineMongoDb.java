package us.kbase.narrativejobservice.db;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.kbase.common.mongo.GetMongoDB;
import us.kbase.common.mongo.exceptions.InvalidHostException;
import us.kbase.common.mongo.exceptions.MongoAuthException;

import com.google.common.collect.Lists;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;

public class ExecEngineMongoDb {
    private DB mongo;
    private Jongo jongo;
    private MongoCollection taskQueue;
    private MongoCollection execApps;
    private MongoCollection execTasks;
    private MongoCollection execLogs;
    private MongoCollection srvProps;
    private volatile boolean wasDbVersionChecked = false;

    private static final Map<String, MongoClient> HOSTS_TO_CLIENT = 
            new HashMap<String, MongoClient>();
    
    public static final String COL_TASK_QUEUE = "task_queue";
    public static final String PK_TASK_QUEUE = "jobid";
    public static final String COL_EXEC_APPS = "exec_apps";
    public static final String PK_EXEC_APPS = "app_job_id";
    public static final String FLD_EXEC_APPS_APP_JOB_STATE = "app_job_state";
    public static final String COL_EXEC_TASKS = "exec_tasks";
    public static final String PK_EXEC_TASKS = "ujs_job_id";
    public static final String COL_EXEC_LOGS = "exec_logs";
    public static final String PK_EXEC_LOGS = "ujs_job_id";
    public static final String COL_SRV_PROPS = "srv_props";
    public static final String PK_SRV_PROPS = "prop_id";
    public static final String SRV_PROP_DB_VERSION = "db_version";
    
    public static final String DB_VERSION = "1.0";

    public ExecEngineMongoDb(String hosts, String db, String user, String pwd,
            Integer mongoReconnectRetry) throws Exception {
        mongo = getDB(hosts, db, user, pwd, 
                mongoReconnectRetry == null ? 0 : mongoReconnectRetry, 10);
        jongo = new Jongo(mongo);
        taskQueue = jongo.getCollection(COL_TASK_QUEUE);
        execApps = jongo.getCollection(COL_EXEC_APPS);
        execTasks = jongo.getCollection(COL_EXEC_TASKS);
        execLogs = jongo.getCollection(COL_EXEC_LOGS);
        srvProps = jongo.getCollection(COL_SRV_PROPS);
        // Indexing
        taskQueue.ensureIndex(String.format("{%s:1}", PK_TASK_QUEUE), "{unique:true}");
        execTasks.ensureIndex(String.format("{%s:1}", PK_EXEC_TASKS), "{unique:true}");
        execApps.ensureIndex(String.format("{%s:1}", PK_EXEC_APPS), "{unique:true}");
        execApps.ensureIndex(String.format("{%s:1}", FLD_EXEC_APPS_APP_JOB_STATE), "{unique:false}");
        execLogs.ensureIndex(String.format("{%s:1}", PK_EXEC_LOGS), "{unique:true}");
        srvProps.ensureIndex(String.format("{%s:1}", PK_SRV_PROPS), "{unique:true}");
    }
    
    public List<QueuedTask> getQueuedTasks() throws Exception {
        return Lists.newArrayList(taskQueue.find("{}").as(QueuedTask.class));
    }

    public void insertQueuedTask(QueuedTask task) throws Exception {
        checkForDbVersion();
        taskQueue.insert(task);
    }
    
    public void deleteQueuedTask(String jobId) throws Exception {
        checkForDbVersion();
        taskQueue.remove(String.format("{%s:#}", PK_TASK_QUEUE), jobId);
    }

    public void insertExecApp(ExecApp execApp) throws Exception {
        checkForDbVersion();
        execApps.insert(execApp);
    }
    
    public ExecApp getExecApp(String appJobId) throws Exception {
        List<ExecApp> ret = Lists.newArrayList(execApps.find(
                String.format("{%s:#}", PK_EXEC_APPS), appJobId).as(ExecApp.class));
        return ret.size() > 0 ? ret.get(0) : null;
    }
    
    public void updateExecAppData(String appJobId, String appJobState, 
            String appStateData) throws Exception {
        checkForDbVersion();
        ExecApp execApp = getExecApp(appJobId);
        if (execApp == null)
            throw new IllegalStateException("App id=" + appJobId + " wasn't found in database");
        execApp.setAppJobState(appJobState);
        execApp.setAppStateData(appStateData);
        execApp.setModificationTime(System.currentTimeMillis());
        execApps.update(String.format("{%s:#}", PK_EXEC_APPS), appJobId).with("#", execApp);
    }
    
    public List<ExecApp> getExecAppsWithState(String appJobState) throws Exception {
        return Lists.newArrayList(execApps.find(
                String.format("{%s:#}", FLD_EXEC_APPS_APP_JOB_STATE), appJobState).as(ExecApp.class));
    }
    
    public ExecLog getExecLog(String ujsJobId) throws Exception {
        List<ExecLog> ret = Lists.newArrayList(execLogs.find(
                String.format("{%s:#}", PK_EXEC_LOGS), ujsJobId)
                .projection(String.format("{%s:1,%s:1,%s:1}", PK_EXEC_LOGS,
                        "original_line_count", "stored_line_count")).as(ExecLog.class));
        return ret.size() > 0 ? ret.get(0) : null;
    }

    public void insertExecLog(ExecLog execLog) throws Exception {
        checkForDbVersion();
        execLogs.insert(execLog);
    }

    public void insertExecLogs(List<ExecLog> execLogList) throws Exception {
        checkForDbVersion();
        Object[] execLogArray = execLogList.toArray(new Object[execLogList.size()]);
        execLogs.insert(execLogArray);
    }

    public void updateExecLogLines(String ujsJobId, int newLineCount, 
            List<ExecLogLine> newLines) throws Exception {
        checkForDbVersion();
        execLogs.update(String.format("{%s:#}", PK_EXEC_LOGS), ujsJobId).with(
                String.format("{$set:{%s:#,%s:#},$push:{%s:{$each:#}}}", 
                        "original_line_count", "stored_line_count", "lines"), 
                        newLineCount, newLineCount, newLines);
    }

    public void updateExecLogOriginalLineCount(String ujsJobId, int newLineCount) throws Exception {
        checkForDbVersion();
        execLogs.update(String.format("{%s:#}", PK_EXEC_LOGS), ujsJobId).with(
                String.format("{$set:{%s:#}}", "original_line_count"), newLineCount);
    }
    
    public List<ExecLogLine> getExecLogLines(String ujsJobId, int from, int count) throws Exception {
        return execLogs.findOne(String.format("{%s:#}", PK_EXEC_LOGS), ujsJobId).projection(
                String.format("{%s:{$slice:[#,#]}}", "lines"), from, count).as(ExecLog.class).getLines();
    }

    public void insertExecTask(ExecTask execTask) throws Exception {
        checkForDbVersion();
        execTasks.insert(execTask);
    }
    
    public ExecTask getExecTask(String ujsJobId) throws Exception {
        List<ExecTask> ret = Lists.newArrayList(execTasks.find(
                String.format("{%s:#}", PK_EXEC_TASKS), ujsJobId).as(ExecTask.class));
        return ret.size() > 0 ? ret.get(0) : null;
    }

    public void updateExecTaskTime(String ujsJobId, boolean finishTime, long time) throws Exception {
        checkForDbVersion();
        execTasks.update(String.format("{%s:#}", PK_EXEC_TASKS), ujsJobId).with(
                String.format("{$set:{%s:#}}", finishTime ? "finish_time" : "exec_start_time"), time);
    }

    public String getServiceProperty(String propId) throws Exception {
        String valueField = "value";
        @SuppressWarnings("rawtypes")
        List<Map> ret = Lists.newArrayList(srvProps.find(String.format("{%s:#}", PK_SRV_PROPS), propId)
                .projection(String.format("{%s:1}", valueField)).as(Map.class));
        if (ret.size() == 0)
            return null;
        return (String)ret.get(0).get(valueField);
    }
    
    public void setServiceProperty(String propId, String value) throws Exception {
        String valueField = "value";
        srvProps.update(String.format("{%s:#}", PK_SRV_PROPS), propId).upsert().with(
                String.format("{$set:{%s:#}}", valueField), value);
    }
    
    private void checkForDbVersion() throws Exception {
        if (!wasDbVersionChecked) {
            synchronized(this) {
                String ver = getServiceProperty(SRV_PROP_DB_VERSION);
                if (ver == null)
                    setServiceProperty(SRV_PROP_DB_VERSION, DB_VERSION);
                wasDbVersionChecked = true;
            }
        }
    }
    
    private synchronized static MongoClient getMongoClient(final String hosts)
            throws UnknownHostException, InvalidHostException {
        //Only make one instance of MongoClient per JVM per mongo docs
        final MongoClient client;
        if (!HOSTS_TO_CLIENT.containsKey(hosts)) {
            // Don't print to stderr
            java.util.logging.Logger.getLogger("com.mongodb")
                    .setLevel(Level.OFF);
            @SuppressWarnings("deprecation")
            final MongoClientOptions opts = MongoClientOptions.builder()
                    .autoConnectRetry(true).build();
            try {
                List<ServerAddress> addr = new ArrayList<ServerAddress>();
                for (String s: hosts.split(","))
                    addr.add(new ServerAddress(s));
                client = new MongoClient(addr, opts);
            } catch (NumberFormatException nfe) {
                //throw a better exception if 10gen ever fixes this
                throw new InvalidHostException(hosts
                        + " is not a valid mongodb host");
            }
            HOSTS_TO_CLIENT.put(hosts, client);
        } else {
            client = HOSTS_TO_CLIENT.get(hosts);
        }
        return client;
    }

    @SuppressWarnings("deprecation")
    public static DB getDB(final String hosts, final String database,
            final String user, final String pwd,
            final int retryCount, final int logIntervalCount)
            throws UnknownHostException, InvalidHostException, IOException,
            MongoAuthException, InterruptedException {
        if (database == null || database.isEmpty()) {
            throw new IllegalArgumentException(
                    "database may not be null or the empty string");
        }
        final DB db = getMongoClient(hosts).getDB(database);
        if (user != null && pwd != null) {
            int retries = 0;
            while (true) {
                try {
                    db.authenticate(user, pwd.toCharArray());
                    break;
                } catch (MongoException.Network men) {
                    if (retries >= retryCount) {
                        throw (IOException) men.getCause();
                    }
                    if (retries % logIntervalCount == 0) {
                        getLogger().info(
                                "Retrying MongoDB connection {}/{}, attempt {}/{}",
                                hosts, database, retries, retryCount);
                    }
                    Thread.sleep(1000);
                }
                retries++;
            }
        }
        try {
            db.getCollectionNames();
        } catch (MongoException me) {
            throw new MongoAuthException("Not authorized for database "
                    + database, me);
        }
        return db;
    }
    
    private static Logger getLogger() {
        return LoggerFactory.getLogger(GetMongoDB.class);
    }
}
