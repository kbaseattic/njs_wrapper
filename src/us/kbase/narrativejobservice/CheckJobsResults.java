
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
 * <p>Original spec-file type: CheckJobsResults</p>
 * <pre>
 * job_states - states of jobs,
 * job_params - parameters of jobs,
 * check_error - this map includes info about errors happening during job checking.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "job_states",
    "job_params",
    "check_error"
})
public class CheckJobsResults {

    @JsonProperty("job_states")
    private Map<String, JobState> jobStates;
    @JsonProperty("job_params")
    private Map<String, RunJobParams> jobParams;
    @JsonProperty("check_error")
    private Map<String, JsonRpcError> checkError;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("job_states")
    public Map<String, JobState> getJobStates() {
        return jobStates;
    }

    @JsonProperty("job_states")
    public void setJobStates(Map<String, JobState> jobStates) {
        this.jobStates = jobStates;
    }

    public CheckJobsResults withJobStates(Map<String, JobState> jobStates) {
        this.jobStates = jobStates;
        return this;
    }

    @JsonProperty("job_params")
    public Map<String, RunJobParams> getJobParams() {
        return jobParams;
    }

    @JsonProperty("job_params")
    public void setJobParams(Map<String, RunJobParams> jobParams) {
        this.jobParams = jobParams;
    }

    public CheckJobsResults withJobParams(Map<String, RunJobParams> jobParams) {
        this.jobParams = jobParams;
        return this;
    }

    @JsonProperty("check_error")
    public Map<String, JsonRpcError> getCheckError() {
        return checkError;
    }

    @JsonProperty("check_error")
    public void setCheckError(Map<String, JsonRpcError> checkError) {
        this.checkError = checkError;
    }

    public CheckJobsResults withCheckError(Map<String, JsonRpcError> checkError) {
        this.checkError = checkError;
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
        return ((((((((("CheckJobsResults"+" [jobStates=")+ jobStates)+", jobParams=")+ jobParams)+", checkError=")+ checkError)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
