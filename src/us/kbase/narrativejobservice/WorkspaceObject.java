
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "workspace_name",
    "object_type",
    "is_input"
})
public class WorkspaceObject {

    @JsonProperty("workspace_name")
    private String workspaceName;
    @JsonProperty("object_type")
    private String objectType;
    @JsonProperty("is_input")
    private Long isInput;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("workspace_name")
    public String getWorkspaceName() {
        return workspaceName;
    }

    @JsonProperty("workspace_name")
    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public WorkspaceObject withWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
        return this;
    }

    @JsonProperty("object_type")
    public String getObjectType() {
        return objectType;
    }

    @JsonProperty("object_type")
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public WorkspaceObject withObjectType(String objectType) {
        this.objectType = objectType;
        return this;
    }

    @JsonProperty("is_input")
    public Long getIsInput() {
        return isInput;
    }

    @JsonProperty("is_input")
    public void setIsInput(Long isInput) {
        this.isInput = isInput;
    }

    public WorkspaceObject withIsInput(Long isInput) {
        this.isInput = isInput;
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
        return ((((((((("WorkspaceObject"+" [workspaceName=")+ workspaceName)+", objectType=")+ objectType)+", isInput=")+ isInput)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
