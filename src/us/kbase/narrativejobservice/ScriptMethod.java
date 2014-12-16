
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
 * <p>Original spec-file type: script_method</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "service_name",
    "method_name",
    "has_files"
})
public class ScriptMethod {

    @JsonProperty("service_name")
    private String serviceName;
    @JsonProperty("method_name")
    private String methodName;
    @JsonProperty("has_files")
    private Long hasFiles;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("service_name")
    public String getServiceName() {
        return serviceName;
    }

    @JsonProperty("service_name")
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public ScriptMethod withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @JsonProperty("method_name")
    public String getMethodName() {
        return methodName;
    }

    @JsonProperty("method_name")
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public ScriptMethod withMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    @JsonProperty("has_files")
    public Long getHasFiles() {
        return hasFiles;
    }

    @JsonProperty("has_files")
    public void setHasFiles(Long hasFiles) {
        this.hasFiles = hasFiles;
    }

    public ScriptMethod withHasFiles(Long hasFiles) {
        this.hasFiles = hasFiles;
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
        return ((((((((("ScriptMethod"+" [serviceName=")+ serviceName)+", methodName=")+ methodName)+", hasFiles=")+ hasFiles)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
