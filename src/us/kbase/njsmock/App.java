
package us.kbase.njsmock;

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
 * <p>Original spec-file type: app</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "app_run_id",
    "steps"
})
public class App {

    @JsonProperty("app_run_id")
    private String appRunId;
    @JsonProperty("steps")
    private List<Step> steps;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("app_run_id")
    public String getAppRunId() {
        return appRunId;
    }

    @JsonProperty("app_run_id")
    public void setAppRunId(String appRunId) {
        this.appRunId = appRunId;
    }

    public App withAppRunId(String appRunId) {
        this.appRunId = appRunId;
        return this;
    }

    @JsonProperty("steps")
    public List<Step> getSteps() {
        return steps;
    }

    @JsonProperty("steps")
    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public App withSteps(List<Step> steps) {
        this.steps = steps;
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
        return ((((((("App"+" [appRunId=")+ appRunId)+", steps=")+ steps)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
