package us.kbase.common.taskqueue2;

public class Task {
	private String jobId;
	private Object params;
	private String authToken;
	private String outRef;
	
	public Task(String jobId, Object params, String authToken, String outRef) {
		this.jobId = jobId;
		this.params = params;
		this.authToken = authToken;
		this.outRef = outRef;
	}
	
	public String getJobId() {
		return jobId;
	}
	
	public Object getParams() {
		return params;
	}
	
	public String getAuthToken() {
		return authToken;
	}
	
	public String getOutRef() {
		return outRef;
	}
}
