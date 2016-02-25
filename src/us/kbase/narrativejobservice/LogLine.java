
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
 * <p>Original spec-file type: LogLine</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "line",
    "is_error"
})
public class LogLine {

    @JsonProperty("line")
    private String line;
    @JsonProperty("is_error")
    private Long isError;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("line")
    public String getLine() {
        return line;
    }

    @JsonProperty("line")
    public void setLine(String line) {
        this.line = line;
    }

    public LogLine withLine(String line) {
        this.line = line;
        return this;
    }

    @JsonProperty("is_error")
    public Long getIsError() {
        return isError;
    }

    @JsonProperty("is_error")
    public void setIsError(Long isError) {
        this.isError = isError;
    }

    public LogLine withIsError(Long isError) {
        this.isError = isError;
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
        return ((((((("LogLine"+" [line=")+ line)+", isError=")+ isError)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
