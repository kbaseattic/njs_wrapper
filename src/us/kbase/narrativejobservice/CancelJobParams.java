
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
 * <p>Original spec-file type: CancelJobParams</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "job_id"
})
public class CancelJobParams {

    @JsonProperty("job_id")
    private String jobId;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("job_id")
    public String getJobId() {
        return jobId;
    }

    @JsonProperty("job_id")
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public CancelJobParams withJobId(String jobId) {
        this.jobId = jobId;
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
        return ((((("CancelJobParams"+" [jobId=")+ jobId)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
