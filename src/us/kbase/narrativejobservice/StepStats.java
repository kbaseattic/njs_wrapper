
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
 * <p>Original spec-file type: step_stats</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "creation_time",
    "exec_start_time",
    "finish_time"
})
public class StepStats {

    @JsonProperty("creation_time")
    private Long creationTime;
    @JsonProperty("exec_start_time")
    private Long execStartTime;
    @JsonProperty("finish_time")
    private Long finishTime;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("creation_time")
    public Long getCreationTime() {
        return creationTime;
    }

    @JsonProperty("creation_time")
    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public StepStats withCreationTime(Long creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    @JsonProperty("exec_start_time")
    public Long getExecStartTime() {
        return execStartTime;
    }

    @JsonProperty("exec_start_time")
    public void setExecStartTime(Long execStartTime) {
        this.execStartTime = execStartTime;
    }

    public StepStats withExecStartTime(Long execStartTime) {
        this.execStartTime = execStartTime;
        return this;
    }

    @JsonProperty("finish_time")
    public Long getFinishTime() {
        return finishTime;
    }

    @JsonProperty("finish_time")
    public void setFinishTime(Long finishTime) {
        this.finishTime = finishTime;
    }

    public StepStats withFinishTime(Long finishTime) {
        this.finishTime = finishTime;
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
        return ((((((((("StepStats"+" [creationTime=")+ creationTime)+", execStartTime=")+ execStartTime)+", finishTime=")+ finishTime)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
