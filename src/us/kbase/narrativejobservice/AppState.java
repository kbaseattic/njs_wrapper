
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
 * mapping<string, string> step_job_ids;
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
    "step_errors"
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
        return ((((((((((((("AppState"+" [jobId=")+ jobId)+", jobState=")+ jobState)+", runningStepId=")+ runningStepId)+", stepOutputs=")+ stepOutputs)+", stepErrors=")+ stepErrors)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
