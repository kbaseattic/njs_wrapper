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
    @JsonProperty("parent_job_id")
    private String parentJobId;


    //njsJobStatus
    @JsonProperty("last_update")
    private String lastUpdate;

    @JsonProperty("last_update")
    public String getLastUpdate() {
        return lastUpdate;
    }
    @JsonProperty("last_update")
    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @JsonProperty("job_stage")
    private String jobStage;

    @JsonProperty("job_stage")
    public String getJobStage() {
        return jobStage;
    }
    @JsonProperty("job_stage")
    public void setJobStage(String jobStage) {
        this.jobStage = jobStage;
    }

    @JsonProperty("job_status")
    private String jobStatus;

    @JsonProperty("job_status")
    public String getJobStatus() {
        return jobStatus;
    }
    @JsonProperty("job_status")
    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    @JsonProperty("job_progress")
    private long jobProgress;

    @JsonProperty("job_progress")
    public long getJobProgress() {
        return jobProgress;
    }
    @JsonProperty("job_progress")
    public void setJobProgress(long jobProgress) {
        this.jobProgress = jobProgress;
    }

    @JsonProperty("job_estimated_completion")
    private String jobEstimateCompletion;

    @JsonProperty("job_estimated_completion")
    public String getJobEstimateCompletion() {
        return jobEstimateCompletion;
    }

    @JsonProperty("job_estimated_completion")
    public void setJobEstimateCompletion(String jobEstimateCompletion) {
        this.jobEstimateCompletion = jobEstimateCompletion;
    }

    @JsonProperty("job_complete")
    private long jobComplete;

    @JsonProperty("job_complete")
    public long getJobComplete() {
        return jobComplete;
    }

    @JsonProperty("job_complete")
    public void setJobComplete(long jobComplete) {
        this.jobComplete = jobComplete;
    }

    @JsonProperty("job_error")
    private long jobError;

    @JsonProperty("job_error")
    public long getJobError() {
        return jobError;
    }

    @JsonProperty("job_error")
    public void setJobError(long jobError) {
        this.jobError = jobError;
    }











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
    public void setSchedulerType(String schedulerType) {
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
        this.lastJobState = lastJobState;
    }

    @JsonProperty("parent_job_id")
    public String getParentJobId() {
        return parentJobId;
    }

    @JsonProperty("parent_job_id")
    public void setParentJobId(String parentJobId) {
        this.parentJobId = parentJobId;
    }
}
