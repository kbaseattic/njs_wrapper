
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
 * <p>Original spec-file type: python_backend_method</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "python_class",
    "method_name"
})
public class PythonBackendMethod {

    @JsonProperty("python_class")
    private String pythonClass;
    @JsonProperty("method_name")
    private String methodName;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("python_class")
    public String getPythonClass() {
        return pythonClass;
    }

    @JsonProperty("python_class")
    public void setPythonClass(String pythonClass) {
        this.pythonClass = pythonClass;
    }

    public PythonBackendMethod withPythonClass(String pythonClass) {
        this.pythonClass = pythonClass;
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

    public PythonBackendMethod withMethodName(String methodName) {
        this.methodName = methodName;
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
        return ((((((("PythonBackendMethod"+" [pythonClass=")+ pythonClass)+", methodName=")+ methodName)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
