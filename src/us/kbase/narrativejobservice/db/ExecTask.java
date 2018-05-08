package us.kbase.narrativejobservice.db;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

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
    @JsonProperty("job_input")
    private Map<String, Object> jobInput;
    @JsonProperty("job_output")
    private Map<String, Object> jobOutput;
    @JsonProperty("scheduler_type")
    private String schedulerType;
    @JsonProperty("task_id")
    private String taskId;
    @JsonProperty("last_job_state")
    private String lastJobState;

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

    @JsonProperty("job_input")
    public Map<String, Object> getJobInput() {
        return jobInput;
    }

    @JsonProperty("job_input")
    public void setJobInput(Map<String, Object> jobInput) {
        this.jobInput = jobInput;
    }

    @JsonProperty("job_output")
    public Map<String, Object> getJobOutput() {
        return jobOutput;
    }

    @JsonProperty("job_output")
    public void setJobOutput(Map<String, Object> jobOutput) {
        this.jobOutput = jobOutput;
    }

    @JsonProperty("schedulerType")
    public String getSchedulerType() {
        return schedulerType;
    }

    @JsonProperty("schedulerType")
    public void setSchdulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    @JsonProperty("task_id")
    public String getTaskId() {
        return taskId;
    }

    @JsonProperty("task_id")
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @JsonProperty("last_job_state")
    public String getLastJobState() {
        return lastJobState;
    }

    @JsonProperty("last_job_state")
    public void setLastJobState(String lastJobState) {
        this.lastJobState = taskId;
    }
}
