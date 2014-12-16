
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
 * <p>Original spec-file type: step_parameter</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "label",
    "value",
    "step_source",
    "is_workspace_id",
    "ws_object"
})
public class StepParameter {

    @JsonProperty("label")
    private String label;
    @JsonProperty("value")
    private String value;
    @JsonProperty("step_source")
    private String stepSource;
    @JsonProperty("is_workspace_id")
    private Long isWorkspaceId;
    /**
     * <p>Original spec-file type: workspace_object</p>
     * <pre>
     * label - label of parameter, can be empty string for positional parameters
     * value - value of parameter
     * step_source - step_id that parameter derives from
     * is_workspace_id - parameter is a workspace id (value is object name)
     * # the below are only used if is_workspace_id is true
     *     is_input - parameter is an input (true) or output (false)
     *     workspace_name - name of workspace
     *     object_type - name of object type
     * </pre>
     * 
     */
    @JsonProperty("ws_object")
    private WorkspaceObject wsObject;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    public StepParameter withLabel(String label) {
        this.label = label;
        return this;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }

    public StepParameter withValue(String value) {
        this.value = value;
        return this;
    }

    @JsonProperty("step_source")
    public String getStepSource() {
        return stepSource;
    }

    @JsonProperty("step_source")
    public void setStepSource(String stepSource) {
        this.stepSource = stepSource;
    }

    public StepParameter withStepSource(String stepSource) {
        this.stepSource = stepSource;
        return this;
    }

    @JsonProperty("is_workspace_id")
    public Long getIsWorkspaceId() {
        return isWorkspaceId;
    }

    @JsonProperty("is_workspace_id")
    public void setIsWorkspaceId(Long isWorkspaceId) {
        this.isWorkspaceId = isWorkspaceId;
    }

    public StepParameter withIsWorkspaceId(Long isWorkspaceId) {
        this.isWorkspaceId = isWorkspaceId;
        return this;
    }

    /**
     * <p>Original spec-file type: workspace_object</p>
     * <pre>
     * label - label of parameter, can be empty string for positional parameters
     * value - value of parameter
     * step_source - step_id that parameter derives from
     * is_workspace_id - parameter is a workspace id (value is object name)
     * # the below are only used if is_workspace_id is true
     *     is_input - parameter is an input (true) or output (false)
     *     workspace_name - name of workspace
     *     object_type - name of object type
     * </pre>
     * 
     */
    @JsonProperty("ws_object")
    public WorkspaceObject getWsObject() {
        return wsObject;
    }

    /**
     * <p>Original spec-file type: workspace_object</p>
     * <pre>
     * label - label of parameter, can be empty string for positional parameters
     * value - value of parameter
     * step_source - step_id that parameter derives from
     * is_workspace_id - parameter is a workspace id (value is object name)
     * # the below are only used if is_workspace_id is true
     *     is_input - parameter is an input (true) or output (false)
     *     workspace_name - name of workspace
     *     object_type - name of object type
     * </pre>
     * 
     */
    @JsonProperty("ws_object")
    public void setWsObject(WorkspaceObject wsObject) {
        this.wsObject = wsObject;
    }

    public StepParameter withWsObject(WorkspaceObject wsObject) {
        this.wsObject = wsObject;
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
        return ((((((((((((("StepParameter"+" [label=")+ label)+", value=")+ value)+", stepSource=")+ stepSource)+", isWorkspaceId=")+ isWorkspaceId)+", wsObject=")+ wsObject)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
