package us.kbase.narrativejobservice.db;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecTask {
    @JsonProperty("ujs_job_id")
    private String ujsJobId;
    @JsonProperty("awe_job_id")
    private String aweJobId;
    @JsonProperty("input_shock_id")
    private String inputShockId;
    @JsonProperty("output_shock_id")
    private String outputShockId;
    @JsonProperty("app_job_id")
    private String appJobId;
    @JsonProperty("creation_time")
    private Long creationTime;
    @JsonProperty("exec_start_time")
    private Long execStartTime;
    @JsonProperty("finish_time")
    private Long finishTime;
    
    @JsonProperty("ujs_job_id")
    public String getUjsJobId() {
        return ujsJobId;
    }
    
    @JsonProperty("ujs_job_id")
    public void setUjsJobId(String ujsJobId) {
        this.ujsJobId = ujsJobId;
    }
    
    @JsonProperty("awe_job_id")
    public String getAweJobId() {
        return aweJobId;
    }
    
    @JsonProperty("awe_job_id")
    public void setAweJobId(String aweJobId) {
        this.aweJobId = aweJobId;
    }
    
    @JsonProperty("input_shock_id")
    public String getInputShockId() {
        return inputShockId;
    }
    
    @JsonProperty("input_shock_id")
    public void setInputShockId(String inputShockId) {
        this.inputShockId = inputShockId;
    }
    
    @JsonProperty("output_shock_id")
    public String getOutputShockId() {
        return outputShockId;
    }
    
    @JsonProperty("output_shock_id")
    public void setOutputShockId(String outputShockId) {
        this.outputShockId = outputShockId;
    }
    
    @JsonProperty("app_job_id")
    public String getAppJobId() {
        return appJobId;
    }
    
    @JsonProperty("app_job_id")
    public void setAppJobId(String appJobId) {
        this.appJobId = appJobId;
    }
    
    @JsonProperty("creation_time")
    public Long getCreationTime() {
        return creationTime;
    }
    
    @JsonProperty("creation_time")
    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }
    
    @JsonProperty("exec_start_time")
    public Long getExecStartTime() {
        return execStartTime;
    }
    
    @JsonProperty("exec_start_time")
    public void setExecStartTime(Long execStartTime) {
        this.execStartTime = execStartTime;
    }
    
    @JsonProperty("finish_time")
    public Long getFinishTime() {
        return finishTime;
    }
    
    @JsonProperty("finish_time")
    public void setFinishTime(Long finishTime) {
        this.finishTime = finishTime;
    }
}
