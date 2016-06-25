
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
 * <p>Original spec-file type: UpdateJobParams</p>
 * <pre>
 * is_started - optional flag marking job as started (and triggering exec_start_time
 *     statistics to be stored).
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "job_id",
    "is_started"
})
public class UpdateJobParams {

    @JsonProperty("job_id")
    private String jobId;
    @JsonProperty("is_started")
    private Long isStarted;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("job_id")
    public String getJobId() {
        return jobId;
    }

    @JsonProperty("job_id")
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public UpdateJobParams withJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    @JsonProperty("is_started")
    public Long getIsStarted() {
        return isStarted;
    }

    @JsonProperty("is_started")
    public void setIsStarted(Long isStarted) {
        this.isStarted = isStarted;
    }

    public UpdateJobParams withIsStarted(Long isStarted) {
        this.isStarted = isStarted;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return ((((((("UpdateJobParams"+" [jobId=")+ jobId)+", isStarted=")+ isStarted)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
