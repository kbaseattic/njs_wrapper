
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
 * <p>Original spec-file type: CheckJobCanceledResult</p>
 * <pre>
 * job_id - id of job running method
 * finished - indicates whether job is done (including error/cancel cases) or not
 * canceled - whether the job is canceled or not.
 * ujs_url - url of UserAndJobState service used by job service
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "job_id",
    "finished",
    "canceled",
    "ujs_url"
})
public class CheckJobCanceledResult {

    @JsonProperty("job_id")
    private String jobId;
    @JsonProperty("finished")
    private Long finished;
    @JsonProperty("canceled")
    private Long canceled;
    @JsonProperty("ujs_url")
    private String ujsUrl;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("job_id")
    public String getJobId() {
        return jobId;
    }

    @JsonProperty("job_id")
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public CheckJobCanceledResult withJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    @JsonProperty("finished")
    public Long getFinished() {
        return finished;
    }

    @JsonProperty("finished")
    public void setFinished(Long finished) {
        this.finished = finished;
    }

    public CheckJobCanceledResult withFinished(Long finished) {
        this.finished = finished;
        return this;
    }

    @JsonProperty("canceled")
    public Long getCanceled() {
        return canceled;
    }

    @JsonProperty("canceled")
    public void setCanceled(Long canceled) {
        this.canceled = canceled;
    }

    public CheckJobCanceledResult withCanceled(Long canceled) {
        this.canceled = canceled;
        return this;
    }

    @JsonProperty("ujs_url")
    public String getUjsUrl() {
        return ujsUrl;
    }

    @JsonProperty("ujs_url")
    public void setUjsUrl(String ujsUrl) {
        this.ujsUrl = ujsUrl;
    }

    public CheckJobCanceledResult withUjsUrl(String ujsUrl) {
        this.ujsUrl = ujsUrl;
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
        return ((((((((((("CheckJobCanceledResult"+" [jobId=")+ jobId)+", finished=")+ finished)+", canceled=")+ canceled)+", ujsUrl=")+ ujsUrl)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
