package us.kbase.narrativejobservice.db;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecApp {
    @JsonProperty("app_job_id")
    private String appJobId;
    @JsonProperty("app_job_state")
    private String appJobState;
    @JsonProperty("app_state_data")
    private String appStateData;
    @JsonProperty("creation_time")
    private Long creationTime;
    @JsonProperty("modification_time")
    private Long modificationTime;
    
    @JsonProperty("app_job_id")
    public String getAppJobId() {
        return appJobId;
    }
    
    @JsonProperty("app_job_id")
    public void setAppJobId(String appJobId) {
        this.appJobId = appJobId;
    }
    
    @JsonProperty("app_job_state")
    public String getAppJobState() {
        return appJobState;
    }
    
    @JsonProperty("app_job_state")
    public void setAppJobState(String appJobState) {
        this.appJobState = appJobState;
    }
    
    @JsonProperty("app_state_data")
    public String getAppStateData() {
        return appStateData;
    }
    
    @JsonProperty("app_state_data")
    public void setAppStateData(String appStateData) {
        this.appStateData = appStateData;
    }
    
    @JsonProperty("creation_time")
    public Long getCreationTime() {
        return creationTime;
    }
    
    @JsonProperty("creation_time")
    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }
    
    @JsonProperty("modification_time")
    public Long getModificationTime() {
        return modificationTime;
    }
    
    @JsonProperty("modification_time")
    public void setModificationTime(Long modificationTime) {
        this.modificationTime = modificationTime;
    }
}
