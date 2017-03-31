
package us.kbase.narrativejobservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: CheckJobsParams</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "job_ids",
    "with_job_params"
})
public class CheckJobsParams {

    @JsonProperty("job_ids")
    private List<String> jobIds;
    @JsonProperty("with_job_params")
    private Long withJobParams;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("job_ids")
    public List<String> getJobIds() {
        return jobIds;
    }

    @JsonProperty("job_ids")
    public void setJobIds(List<String> jobIds) {
        this.jobIds = jobIds;
    }

    public CheckJobsParams withJobIds(List<String> jobIds) {
        this.jobIds = jobIds;
        return this;
    }

    @JsonProperty("with_job_params")
    public Long getWithJobParams() {
        return withJobParams;
    }

    @JsonProperty("with_job_params")
    public void setWithJobParams(Long withJobParams) {
        this.withJobParams = withJobParams;
    }

    public CheckJobsParams withWithJobParams(Long withJobParams) {
        this.withJobParams = withJobParams;
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
        return ((((((("CheckJobsParams"+" [jobIds=")+ jobIds)+", withJobParams=")+ withJobParams)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
