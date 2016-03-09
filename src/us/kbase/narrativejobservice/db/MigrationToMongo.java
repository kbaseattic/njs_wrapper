package us.kbase.narrativejobservice.db;

import java.io.File;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.kbase.common.service.UObject;
import us.kbase.common.taskqueue2.TaskQueueConfig;
import us.kbase.common.utils.DbConn;
import us.kbase.narrativejobservice.AppState;
import us.kbase.narrativejobservice.LogLine;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.narrativejobservice.RunAppBuilder;

public class MigrationToMongo {
    public static final String DERBY_DB_NAME = "GenomeCmpDb";
    public static final String QUEUE_TABLE_NAME = "task_queue";
    public static final String DONE_TABLE_NAME = "done";
    public static final String AWE_APPS_TABLE_NAME = "awe_apps";
    public static final String AWE_TASKS_TABLE_NAME = "awe_tasks";
    public static final String AWE_LOGS_TABLE_NAME = "awe_logs";

    public static final int OLD_MAX_LOG_LINE_LENGTH = 10000;
    public static final long OLD_MAX_APP_SIZE = 30000;

    public static DbConn getDbConnection(Map<String, String> config) throws ClassNotFoundException, SQLException {
        String ret = config.get(NarrativeJobServiceServer.CFG_PROP_QUEUE_DB_DIR);
        if (ret == null)
            throw new IllegalStateException("Parameter " + NarrativeJobServiceServer.CFG_PROP_QUEUE_DB_DIR + 
                    " is not defined in configuration");
        File dir = new File(ret);
        if (!dir.exists())
            dir.mkdirs();
        return getDbConnection(dir);
    }

    public static DbConn getDbConnection(File dbParentDir) throws ClassNotFoundException, SQLException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        File dbDir = new File(dbParentDir, DERBY_DB_NAME);
        String url = "jdbc:derby:" + dbDir.getParent() + "/" + dbDir.getName();
        if (!dbDir.exists())
            url += ";create=true";
        return new DbConn(DriverManager.getConnection(url));
    }

    public static void migrate(TaskQueueConfig config, final ExecEngineMongoDb target) throws Exception {
        if (config.getQueueDbDir() == null || !config.getQueueDbDir().exists())
            return;
        DbConn source = getDbConnection(config.getQueueDbDir());
        try {
            if (!source.checkTable(QUEUE_TABLE_NAME))
                return;
            if (source.checkTable(DONE_TABLE_NAME))
                return;
            // Queue
            List<QueuedTask> tasks = source.collect("select jobid,type,params,auth,outref from " + 
                    QUEUE_TABLE_NAME, new DbConn.SqlLoader<QueuedTask>() {
                @Override
                public QueuedTask collectRow(ResultSet rs) throws SQLException {
                    QueuedTask ret = new QueuedTask();
                    ret.setJobid(rs.getString("jobid"));
                    ret.setType(rs.getString("type"));
                    ret.setParams(rs.getString("params"));
                    ret.setAuth(rs.getString("auth"));
                    ret.setOutref(rs.getString("outref"));
                    return ret;
                }
            });
            for (QueuedTask task : tasks)
                target.insertQueuedTask(task);
            System.out.println("Queued tasks: " + tasks.size());
            // Apps
            List<String> appIds = source.collect("select app_job_id,app_job_state,app_state_data," +
            		"creation_time,modification_time from " + 
                    AWE_APPS_TABLE_NAME, new DbConn.SqlLoader<String>() {
                @Override
                public String collectRow(ResultSet rs) throws SQLException {
                    try {
                        ExecApp app = new ExecApp();
                        app.setAppJobId(rs.getString("app_job_id"));
                        app.setAppJobState(rs.getString("app_job_state"));
                        app.setAppStateData(rs.getString("app_state_data"));
                        app.setCreationTime(rs.getLong("creation_time"));
                        app.setModificationTime(rs.getLong("modification_time"));
                        target.insertExecApp(app);
                        return app.getAppJobId();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            });
            System.out.println("Apps: " + appIds.size());
            // Tasks
            List<String> taskIds = source.collect("select ujs_job_id,awe_job_id,input_shock_id," +
            		"output_shock_id,creation_time,app_job_id,exec_start_time,finish_time from " + 
                    AWE_TASKS_TABLE_NAME, new DbConn.SqlLoader<String>() {
                @Override
                public String collectRow(ResultSet rs) throws SQLException {
                    try {
                        ExecTask dbTask = new ExecTask();
                        dbTask.setUjsJobId(rs.getString("ujs_job_id"));
                        dbTask.setAweJobId(rs.getString("awe_job_id"));
                        dbTask.setInputShockId(rs.getString("input_shock_id"));
                        dbTask.setOutputShockId(rs.getString("output_shock_id"));
                        dbTask.setCreationTime(rs.getLong("creation_time"));
                        dbTask.setAppJobId(rs.getString("app_job_id"));
                        long execStartTime = rs.getLong("exec_start_time");
                        if (!rs.wasNull())
                            dbTask.setExecStartTime(execStartTime);
                        long finishTime = rs.getLong("finish_time");
                        if (!rs.wasNull())
                            dbTask.setFinishTime(finishTime);
                        target.insertExecTask(dbTask);
                        return dbTask.getUjsJobId();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            });
            System.out.println("Tasks: " + taskIds.size());
            // Logs
            int logCount = 0;
            int maxLogs = 0;
            for (String taskId : taskIds) {
                String sql = "select line_pos,line,is_error from " + NarrativeJobServiceServer.AWE_LOGS_TABLE_NAME + 
                        " where ujs_job_id=? order by line_pos";
                List<ExecLogLine> lines = source.collect(sql, new DbConn.SqlLoader<ExecLogLine>() {
                    @Override
                    public ExecLogLine collectRow(ResultSet rs) throws SQLException {
                        ExecLogLine ret = new ExecLogLine();
                        ret.setLinePos(rs.getInt(1));
                        String text = rs.getString(2);
                        if (text.length() > RunAppBuilder.MAX_LOG_LINE_LENGTH)
                            text = text.substring(0, RunAppBuilder.MAX_LOG_LINE_LENGTH - 3) + "...";
                        ret.setLine(text);
                        ret.setIsError(rs.getInt(3) == 1);
                        return ret;
                    }
                }, taskId);
                ExecLog dbLog = new ExecLog();
                dbLog.setUjsJobId(taskId);
                dbLog.setOriginalLineCount(lines.size());
                dbLog.setStoredLineCount(lines.size());
                dbLog.setLines(lines);
                try {
                    target.insertExecLog(dbLog);
                } catch (Exception ex) {
                    dbLog = new ExecLog();
                    dbLog.setUjsJobId(taskId);
                    dbLog.setOriginalLineCount(0);
                    dbLog.setStoredLineCount(0);
                    dbLog.setLines(new ArrayList<ExecLogLine>());
                    target.insertExecLog(dbLog);
                    int partSize = 1000;
                    int partCount = (lines.size() + partSize - 1) / partSize;
                    try {
                        for (int i = 0; i < partCount; i++) {
                            int newLineCount = Math.min((i + 1) * partSize, lines.size());
                            List<ExecLogLine> part = lines.subList(i * partSize, newLineCount);
                            target.updateExecLogLines(taskId, newLineCount, part);
                        }
                    } catch (Exception ex2) {
                        target.updateExecLogOriginalLineCount(taskId, lines.size());
                    }
                }
                logCount += lines.size();
                if (maxLogs < lines.size())
                    maxLogs = lines.size();
            }
            System.out.println("Logs: " + logCount);
            System.out.println("Max.logs: " + maxLogs);
            // All is done.
            //source.exec("create table " + DONE_TABLE_NAME + " (id varchar(100) primary key)");
        } finally {
            source.getConnection().close();
        }
    }
    
    public static void constructOldDb(DbConn conn) throws Exception {
        if (!conn.checkTable(QUEUE_TABLE_NAME)) {
            conn.exec("create table " + QUEUE_TABLE_NAME + " (" +
                    "jobid varchar(100) primary key," +
                    "type varchar(100)," +
                    "params clob(100 m)," +
                    "auth varchar(1000)," +
                    "outref varchar(1000)" +
                    ")");
        }
        if (!conn.checkTable(NarrativeJobServiceServer.AWE_TASK_TABLE_NAME)) {
            conn.exec("create table " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " (" +
                    "ujs_job_id varchar(100) primary key," +
                    "awe_job_id varchar(100)," +
                    "input_shock_id varchar(100)," +
                    "output_shock_id varchar(100)," +
                    "creation_time bigint," +
                    "app_job_id varchar(100)," +
                    "exec_start_time bigint," +
                    "finish_time bigint" +
                    ")");
            conn.exec("create index " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + "_app_job_id on " + 
                    NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " (app_job_id)");
        }
        if (!conn.checkTable(NarrativeJobServiceServer.AWE_LOGS_TABLE_NAME)) {
            conn.exec("create table " + NarrativeJobServiceServer.AWE_LOGS_TABLE_NAME + " (" +
                    "ujs_job_id varchar(100)," +
                    "line_pos integer," +
                    "line long varchar," +
                    "is_error smallint," +
                    "primary key (ujs_job_id, line_pos)" +
                    ")");
        }
        if (!conn.checkTable(NarrativeJobServiceServer.AWE_APPS_TABLE_NAME)) {
            conn.exec("create table " + NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + " (" +
                    "app_job_id varchar(100) primary key," +
                    "app_job_state varchar(100)," +
                    "app_state_data long varchar," +
                    "creation_time bigint," +
                    "modification_time bigint" +
                    ")");
            conn.exec("create index " + NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + "_app_job_state on " + 
                    NarrativeJobServiceServer.AWE_APPS_TABLE_NAME + " (app_job_state)");
        }
    }
    
    public static void addOldTaskIntoQueue(DbConn conn, TaskQueueConfig config, Object params, 
            String authToken, String description, String outRef) throws Exception {
        String jobId = config.getJobStatuses().createAndStartJob(authToken, "queued", 
                description, "none", null);
        String type = params.getClass().getName();
        String paramsJson = new ObjectMapper().writeValueAsString(params);
        conn.exec("insert into " + QUEUE_TABLE_NAME + " (jobid,type,params,auth,outref)" +
        		" values (?,?,?,?,?)", jobId, type, paramsJson, authToken, outRef);
    }
    
    public static void addOldAweTaskDescription(DbConn conn, String ujsJobId, String aweJobId, String inputShockId, 
            String outputShockId, String appJobId) throws Exception {
        conn.exec("insert into " + AWE_TASKS_TABLE_NAME + 
                " (ujs_job_id,awe_job_id,input_shock_id,output_shock_id,creation_time," +
                "app_job_id) values (?,?,?,?,?,?)", 
                ujsJobId, aweJobId, inputShockId, outputShockId, System.currentTimeMillis(), appJobId);
    }

    public static void updateOldAweTaskExecTime(DbConn conn, String ujsJobId, boolean isFinishTimeField) throws Exception {
        String timeField = isFinishTimeField ? "finish_time" : "exec_start_time";
        conn.exec("update " + NarrativeJobServiceServer.AWE_TASK_TABLE_NAME + " set " + timeField + "=? " +
                "where ujs_job_id=?", System.currentTimeMillis(), ujsJobId);
    }

    public static void addOldAweApp(DbConn conn, AppState appState, long startTime) throws Exception {
        conn.exec("insert into " + AWE_APPS_TABLE_NAME + " " +
                "(app_job_id,app_job_state,app_state_data,creation_time, modification_time) " +
                "values (?,?,?,?,?)", appState.getJobId(), appState.getJobState(),
                UObject.getMapper().writeValueAsString(appState), startTime, startTime);
    }
    
    public static synchronized void updateAppState(DbConn conn, AppState appState) throws Exception {
        String appData = UObject.getMapper().writeValueAsString(appState);
        if (appData.length() > OLD_MAX_APP_SIZE)
            throw new IllegalStateException("App data is too large (>" + OLD_MAX_APP_SIZE + ")");
        conn.exec("update " + AWE_APPS_TABLE_NAME + " set " +
                "app_job_state=?, app_state_data=?, modification_time=? where " +
                "app_job_id=?", appState.getJobState(), appData, 
                System.currentTimeMillis(), appState.getJobId());
    }

    public static void addOldAweLogs(DbConn conn, String ujsJobId, int linePos, LogLine line) throws Exception {
        String text = line.getLine();
        if (text.length() > OLD_MAX_LOG_LINE_LENGTH)
            text = text.substring(0, OLD_MAX_LOG_LINE_LENGTH - 3) + "...";
        conn.exec("insert into " + AWE_LOGS_TABLE_NAME + 
                " (ujs_job_id,line_pos,line,is_error) values (?,?,?,?)", 
                ujsJobId, linePos, text, line.getIsError());
    }
}
