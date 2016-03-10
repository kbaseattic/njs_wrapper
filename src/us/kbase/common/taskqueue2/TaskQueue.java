package us.kbase.common.taskqueue2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import us.kbase.auth.AuthToken;
import us.kbase.narrativejobservice.db.ExecEngineMongoDb;
import us.kbase.narrativejobservice.db.QueuedTask;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskQueue {
	private ExecEngineMongoDb db;
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
	
	public TaskQueue(TaskQueueConfig config, RestartChecker rCheck, ExecEngineMongoDb db,
			TaskRunner<?>... runners) throws Exception {
		this.config = config;
		this.rCheck = rCheck;
		this.db = db;
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
	
	public TaskQueueConfig getConfig() {
        return config;
    }
	
	public ExecEngineMongoDb getDb() {
        return db;
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

	private void checkForUnfinishedTasks() throws Exception {
		List<QueuedTask> tasks = db.getQueuedTasks();
		for (QueuedTask dbTask : tasks) {
            // jobid,type,params,auth,outref
            Class<?> type = Class.forName(dbTask.getType());
            Object params = new ObjectMapper().readValue(dbTask.getParams(), type);
            Task task = new Task(dbTask.getJobid(), params, dbTask.getAuth(), dbTask.getOutref());
			addTask(task);
		}
		if (tasks.size() > 0 && !rCheck.isInRestartMode()) {
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
		String jobId = createQueuedTaskJob(description, authToken);
		Task task = new Task(jobId, params, authToken, outRef);
		addTask(task);
		storeTaskInDb(task);
		if (!rCheck.isInRestartMode()) {
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
	
	private void storeTaskInDb(Task task) throws Exception {
	    QueuedTask dbTask = new QueuedTask();
        dbTask.setJobid(task.getJobId());
        dbTask.setType(task.getParams().getClass().getName());
        dbTask.setParams(new ObjectMapper().writeValueAsString(task.getParams()));
        dbTask.setAuth(task.getAuthToken());
        dbTask.setOutref(task.getOutRef());
		db.insertQueuedTask(dbTask);
	}
	
	public void deleteTaskFromDb(String jobId) throws Exception {
		db.deleteQueuedTask(jobId);
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
		if (rCheck.isInRestartMode())
			return null;
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
			if (needToStop) {
				System.out.println("Task " + task.getJobId() + " was left for next server start");
				return false;
			}
			if (rCheck.isInRestartMode()) {
				System.out.println("Task " + task.getJobId() + " was left until reboot mode is switched off");
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