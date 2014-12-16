package us.kbase.common.taskqueue2;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import us.kbase.auth.AuthToken;
import us.kbase.common.utils.DbConn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskQueue {
	private DbConn conn;
	private Map<Class<?>, TaskRunner<?>> runners = new HashMap<Class<?>, TaskRunner<?>>();
	private Map<String, Task> taskMap = new HashMap<String, Task>();
	private LinkedList<Task> taskQueue = new LinkedList<Task>();
	private Thread[] allThreads;
	private volatile boolean needToStop = false;
	private final Object idleMonitor = new Object();
	private final TaskQueueConfig config;
	private final RestartChecker rCheck;
	private final Map<String, Integer> userToRunningTaskCount = new HashMap<String, Integer>();
	    
    private static final int MAX_ERROR_MESSAGE_LEN = 190;
    public static final String DERBY_DB_NAME = "GenomeCmpDb";
    public static final String QUEUE_TABLE_NAME = "task_queue";
	
	public TaskQueue(TaskQueueConfig config, RestartChecker rCheck,
			TaskRunner<?>... runners) throws ClassNotFoundException, SQLException {
		this.config = config;
		this.rCheck = rCheck;
		conn = getDbConnection(config.getQueueDbDir());
		if (!conn.checkTable(QUEUE_TABLE_NAME)) {
			conn.exec("create table " + QUEUE_TABLE_NAME + " (" +
					"jobid varchar(100) primary key," +
					"type varchar(100)," +
					"params clob(100 m)," +
					"auth varchar(1000)," +
					"outref varchar(1000)" +
					")");
		}
		allThreads = new Thread[config.getThreadCount()];
		for (int i = 0; i < allThreads.length; i++) {
			allThreads[i] = startNewThread(i);
		}
		for (TaskRunner<?> runner : runners)
			registerRunner(runner);
		checkForUnfinishedTasks();
	}
	
	public synchronized boolean getStoppingMode() {
		return needToStop;
	}
	
	public synchronized int getAllTasks() {
		return taskMap.size();
	}
	
	public synchronized int getQueuedTasks() {
		return taskQueue.size();
	}
	
	public synchronized Map<String, Long> getRunningTasksPerUser() {
		Map<String, Long> ret = new TreeMap<String, Long>();
		for (String user : userToRunningTaskCount.keySet()) {
			Integer val = userToRunningTaskCount.get(user);
			if (val != null)
				ret.put(user, (long)(int)val);
		}
		return ret;
	}
	
	private void registerRunner(TaskRunner<?> runner) {
		runner.init(config, config.getAllConfigProps());
		runners.put(runner.getInputDataType(), runner);
	}

	public static DbConn getDbConnection(File dbParentDir) throws ClassNotFoundException, SQLException {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		File dbDir = new File(dbParentDir, DERBY_DB_NAME);
		String url = "jdbc:derby:" + dbDir.getParent() + "/" + dbDir.getName();
		if (!dbDir.exists())
			url += ";create=true";
		return new DbConn(DriverManager.getConnection(url));
	}
	
	private void checkForUnfinishedTasks() throws SQLException {
		List<Task> tasks = conn.collect("select jobid,type,params,auth,outref from " + 
				QUEUE_TABLE_NAME, new DbConn.SqlLoader<Task>() {
			@Override
			public Task collectRow(ResultSet rs) throws SQLException {
				try {
					Class<?> type = Class.forName(rs.getString("type"));
					String paramsJson = rs.getString("params");
					Object params = new ObjectMapper().readValue(paramsJson, type);
					return new Task(rs.getString("jobid"), params, rs.getString("auth"),
							rs.getString("outref"));
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException(e);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		});
		for (Task task : tasks)
			addTask(task);
		if (tasks.size() > 0) {
			synchronized (idleMonitor) {
				idleMonitor.notifyAll();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> String addTask(T params, String authToken) throws Exception {
		TaskRunner<T> runner = (TaskRunner<T>)runners.get(params.getClass());
		if (runner == null)
			throw new IllegalStateException("Task data type is not supported: " + params.getClass().getName());
		String outRef = runner.getOutRef(params);
		return addTask(params, authToken, runner.getTaskDescription(), outRef);
	}

	public String addTaskForTest(Runnable params, String authToken) throws Exception {
		return addTask(params, authToken, "descr", "out");
	}
	
	private synchronized String addTask(Object params, String authToken, String description, String outRef) throws Exception {
		if (rCheck.isInRestartMode()) {
			stopAllThreads();
			throw new IllegalStateException("Task queue is in reboot mode");
		}
		String jobId = createQueuedTaskJob(description, authToken);
		Task task = new Task(jobId, params, authToken, outRef);
		addTask(task);
		storeTaskInDb(task);
		if (!needToStop) {
			synchronized (idleMonitor) {
				idleMonitor.notifyAll();
			}
		}
		return jobId;
	}

	private synchronized void addTask(Task task) {
		taskQueue.addLast(task);
		taskMap.put(task.getJobId(), task);
	}
	
	private void storeTaskInDb(Task task) throws JsonProcessingException, SQLException {
		String type = task.getParams().getClass().getName();
		String params = new ObjectMapper().writeValueAsString(task.getParams());
		conn.exec("insert into " + QUEUE_TABLE_NAME + " (jobid,type,params,auth,outref) values (?,?,?,?,?)", 
				task.getJobId(), type, params, task.getAuthToken(), task.getOutRef());
	}
	
	private void deleteTaskFromDb(String jobId) throws SQLException {
		conn.exec("delete from " + QUEUE_TABLE_NAME + " where jobid=?", jobId);
	}
	
	private synchronized void removeTask(Task task) {
		String user = getUserForTask(task);
		Integer count = userToRunningTaskCount.get(user);
		if (count != null) {
			if (count == 1) {
				userToRunningTaskCount.remove(user);
			} else {
				userToRunningTaskCount.put(user, count - 1);
			}
		}
		try {
			deleteTaskFromDb(task.getJobId());
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
		taskMap.remove(task.getJobId());
		System.out.println("Task " + task.getJobId() + " was deleted");
	}
	
	protected String getUserForTask(Task task) {
		try {
			return new AuthToken(task.getAuthToken()).getClientId();
		} catch (Exception ex) {
			return "unknown";
		}
	}
	
	public synchronized Task getTask(String jobId) {
		return taskMap.get(jobId);
	}
	
	private synchronized Task gainNewTask() {
		if (taskQueue.size() > 0) {
			Task ret = null;
			int limit = config.getRunningTasksPerUser();
			if (limit <= 0) {
				ret = taskQueue.removeFirst();
			} else {
				for (int i = 0; i < taskQueue.size(); i++) {
					Task task = taskQueue.get(i);
					String user = getUserForTask(task);
					Integer count = userToRunningTaskCount.get(user);
					if (count == null)
						count = 0;
					if (count < limit) {
						userToRunningTaskCount.put(user, count + 1);
						ret = task;
						taskQueue.remove(i);
						break;
					}
				}
			}
			return ret;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private boolean runTask(Task task) {
		String token = task.getAuthToken();
		try {
			changeTaskStateIntoRunning(task, token);
			Object params = task.getParams();
			TaskRunner<Object> runner = (TaskRunner<Object>)runners.get(params.getClass());
			if (runner == null)
				throw new IllegalStateException("Task data type is not supported: " + params.getClass().getName());
			runner.run(token, params, task.getJobId(), task.getOutRef());
			completeTaskState(task, token, null, null);
		} catch (Throwable e) {
			if (!needToStop) {
				if (rCheck.isInRestartMode())
					stopAllThreads();
			}
			if (needToStop) {
				System.out.println("Task " + task.getJobId() + " was left for next server start");
				return false;
			}
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				pw.close();
				String errMsg = null;
				if (e.getMessage() == null) {
					errMsg = e.getClass().getSimpleName();
				} else {
					errMsg = "Error: " + e.getMessage();
				}
				if (errMsg.length() > MAX_ERROR_MESSAGE_LEN)
					errMsg = errMsg.substring(0, MAX_ERROR_MESSAGE_LEN - 3) + "...";
				completeTaskState(task, token, errMsg, sw.toString());
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}
		return true;
	}

	private String createQueuedTaskJob(String description, String token) throws Exception {
		return config.getJobStatuses().createAndStartJob(token, "queued", description, 
				"none", null);
	}

	private void changeTaskStateIntoRunning(Task task, String token) throws Exception {
		config.getJobStatuses().updateJob(task.getJobId(), token, "running", null);
	}

	private void completeTaskState(Task task, String token, String errorMessage, String errorStacktrace) throws Exception {
		if (errorMessage == null) {
			config.getJobStatuses().completeJob(task.getJobId(), token, "done", null, 
					config.getWsUrl(), task.getOutRef());
		} else {
			config.getJobStatuses().completeJob(task.getJobId(), token, errorMessage, 
					errorStacktrace, null, null); 
		}
	}
	
	public void stopAllThreads() {
		if (needToStop)
			return;
		needToStop = true;
		for (Thread t : allThreads)
			t.interrupt();
	}
	
	private Thread startNewThread(final int num) {
		Thread ret = new Thread(
				new Runnable() {
					@Override
					public void run() {
						while (!needToStop) {
							Task task = gainNewTask();
							if (task != null) {
								if (runTask(task))
									removeTask(task);
							} else {
								long ms = 55 * 1000 + (int)(10 * 1000 * Math.random());
								synchronized (idleMonitor) {
									try {
										idleMonitor.wait(ms);
									} catch (InterruptedException e) {
										if (!needToStop)
											e.printStackTrace();
									}
								}
							}
						}
						System.out.println("Task thread " + (num + 1) + " was stoped");
					}
				},"Task thread " + (num + 1));
		ret.start();
		return ret;
	}
}