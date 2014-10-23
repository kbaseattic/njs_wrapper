
package us.kbase.njsmock;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: app_jobs</p>
 * <pre>
 * step_jobs - mapping from step_id to job_id.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "app_job_id",
    "step_job_ids"
})
public class AppJobs {

    @JsonProperty("app_job_id")
    private java.lang.String appJobId;
    @JsonProperty("step_job_ids")
    private Map<String, String> stepJobIds;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("app_job_id")
    public java.lang.String getAppJobId() {
        return appJobId;
    }

    @JsonProperty("app_job_id")
    public void setAppJobId(java.lang.String appJobId) {
        this.appJobId = appJobId;
    }

    public AppJobs withAppJobId(java.lang.String appJobId) {
        this.appJobId = appJobId;
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

    public AppJobs withStepJobIds(Map<String, String> stepJobIds) {
        this.stepJobIds = stepJobIds;
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
        return ((((((("AppJobs"+" [appJobId=")+ appJobId)+", stepJobIds=")+ stepJobIds)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
