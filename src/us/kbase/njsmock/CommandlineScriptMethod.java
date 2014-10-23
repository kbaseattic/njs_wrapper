
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
 * <p>Original spec-file type: commandline_script_method</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "script_name"
})
public class CommandlineScriptMethod {

    @JsonProperty("script_name")
    private String scriptName;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("script_name")
    public String getScriptName() {
        return scriptName;
    }

    @JsonProperty("script_name")
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public CommandlineScriptMethod withScriptName(String scriptName) {
        this.scriptName = scriptName;
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
        return ((((("CommandlineScriptMethod"+" [scriptName=")+ scriptName)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
