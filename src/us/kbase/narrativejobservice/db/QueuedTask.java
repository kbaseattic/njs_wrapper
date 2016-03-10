package us.kbase.narrativejobservice.db;

public class QueuedTask {
	private String jobid;
	private String type;
	private String params;
	private String auth;
	private String outref;
	
	public QueuedTask() {
    }
	
	public String getJobid() {
		return jobid;
	}
	
	public void setJobid(String jobid) {
        this.jobid = jobid;
    }
	
	public String getType() {
        return type;
    }
	
	public void setType(String type) {
        this.type = type;
    }
	
	public String getParams() {
		return params;
	}
	
	public void setParams(String params) {
        this.params = params;
    }
	
	public String getAuth() {
		return auth;
	}
	
	public void setAuth(String auth) {
        this.auth = auth;
    }
	
	public String getOutref() {
		return outref;
	}
	
	public void setOutref(String outref) {
        this.outref = outref;
    }
}
