package us.kbase.common.taskqueue2;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class TaskQueueConfig {
	private int threadCount;
	private File queueDbDir;
	private JobStatuses jobStatuses;
	private String wsUrl;
	private int runningTasksPerUser;
	private Map<String, String> allConfigProps;
	
	public TaskQueueConfig(int threadCount, File queueDbDir, JobStatuses jobStatuses, 
			String wsUrl, int runningTasksPerUser, Map<String, String> allConfigProps) {
		this.threadCount = threadCount;
		this.queueDbDir = queueDbDir;
		this.jobStatuses = jobStatuses;
		this.wsUrl = wsUrl;
		this.runningTasksPerUser = runningTasksPerUser;
		this.allConfigProps = allConfigProps == null ? Collections.<String, String>emptyMap() : 
			Collections.unmodifiableMap(allConfigProps);
	}
	
	public int getThreadCount() {
		return threadCount;
	}

	public File getQueueDbDir() {
		return queueDbDir;
	}
	
	public JobStatuses getJobStatuses() {
		return jobStatuses;
	}
	
	public String getWsUrl() {
		return wsUrl;
	}
	
	public int getRunningTasksPerUser() {
		return runningTasksPerUser;
	}
	
	public Map<String, String> getAllConfigProps() {
		return allConfigProps;
	}
}
