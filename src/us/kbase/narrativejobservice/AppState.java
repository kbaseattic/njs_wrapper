
package us.kbase.narrativejobservice;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: app_state</p>
 * <pre>
 * job_id - id of job running app
 * job_state - 'queued', 'running', 'completed', or 'error'
 * running_step_id - id of step currently running
 * step_outputs - mapping step_id to stdout text produced by step, only for completed or errored steps
 * step_outputs - mapping step_id to stderr text produced by step, only for completed or errored steps
 * step_job_ids - mapping from step_id to job_id.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "job_id",
    "job_state",
    "running_step_id",
    "step_outputs",
    "step_errors",
    "step_job_ids",
    "is_deleted"
})
public class AppState {

    @JsonProperty("job_id")
    private java.lang.String jobId;
    @JsonProperty("job_state")
    private java.lang.String jobState;
    @JsonProperty("running_step_id")
    private java.lang.String runningStepId;
    @JsonProperty("step_outputs")
    private Map<String, String> stepOutputs;
    @JsonProperty("step_errors")
    private Map<String, String> stepErrors;
    @JsonProperty("step_job_ids")
    private Map<String, String> stepJobIds;
    @JsonProperty("is_deleted")
    private Long isDeleted;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("job_id")
    public java.lang.String getJobId() {
        return jobId;
    }

    @JsonProperty("job_id")
    public void setJobId(java.lang.String jobId) {
        this.jobId = jobId;
    }

    public AppState withJobId(java.lang.String jobId) {
        this.jobId = jobId;
        return this;
    }

    @JsonProperty("job_state")
    public java.lang.String getJobState() {
        return jobState;
    }

    @JsonProperty("job_state")
    public void setJobState(java.lang.String jobState) {
        this.jobState = jobState;
    }

    public AppState withJobState(java.lang.String jobState) {
        this.jobState = jobState;
        return this;
    }

    @JsonProperty("running_step_id")
    public java.lang.String getRunningStepId() {
        return runningStepId;
    }

    @JsonProperty("running_step_id")
    public void setRunningStepId(java.lang.String runningStepId) {
        this.runningStepId = runningStepId;
    }

    public AppState withRunningStepId(java.lang.String runningStepId) {
        this.runningStepId = runningStepId;
        return this;
    }

    @JsonProperty("step_outputs")
    public Map<String, String> getStepOutputs() {
        return stepOutputs;
    }

    @JsonProperty("step_outputs")
    public void setStepOutputs(Map<String, String> stepOutputs) {
        this.stepOutputs = stepOutputs;
    }

    public AppState withStepOutputs(Map<String, String> stepOutputs) {
        this.stepOutputs = stepOutputs;
        return this;
    }

    @JsonProperty("step_errors")
    public Map<String, String> getStepErrors() {
        return stepErrors;
    }

    @JsonProperty("step_errors")
    public void setStepErrors(Map<String, String> stepErrors) {
        this.stepErrors = stepErrors;
    }

    public AppState withStepErrors(Map<String, String> stepErrors) {
        this.stepErrors = stepErrors;
        return this;
    }

    @JsonProperty("step_job_ids")
    public Map<String, String> getStepJobIds() {
        return stepJobIds;
    }

    @JsonProperty("step_job_ids")
    public void setStepJobIds(Map<String, String> stepJobIds) {
        this.stepJobIds = stepJobIds;
    }

    public AppState withStepJobIds(Map<String, String> stepJobIds) {
        this.stepJobIds = stepJobIds;
        return this;
    }

    @JsonProperty("is_deleted")
    public Long getIsDeleted() {
        return isDeleted;
    }

    @JsonProperty("is_deleted")
    public void setIsDeleted(Long isDeleted) {
        this.isDeleted = isDeleted;
    }

    public AppState withIsDeleted(Long isDeleted) {
        this.isDeleted = isDeleted;
        return this;
    }

    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public java.lang.String toString() {
        return ((((((((((((((((("AppState"+" [jobId=")+ jobId)+", jobState=")+ jobState)+", runningStepId=")+ runningStepId)+", stepOutputs=")+ stepOutputs)+", stepErrors=")+ stepErrors)+", stepJobIds=")+ stepJobIds)+", isDeleted=")+ isDeleted)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
