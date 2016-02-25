
package us.kbase.narrativejobservice;

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
 * <p>Original spec-file type: GetJobLogsResults</p>
 * <pre>
 * last_line_number - common number of lines (including those in skip_lines 
 *     parameter), this number can be used as next skip_lines value to
 *     skip already loaded lines next time.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "lines",
    "last_line_number"
})
public class GetJobLogsResults {

    @JsonProperty("lines")
    private List<LogLine> lines;
    @JsonProperty("last_line_number")
    private Long lastLineNumber;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("lines")
    public List<LogLine> getLines() {
        return lines;
    }

    @JsonProperty("lines")
    public void setLines(List<LogLine> lines) {
        this.lines = lines;
    }

    public GetJobLogsResults withLines(List<LogLine> lines) {
        this.lines = lines;
        return this;
    }

    @JsonProperty("last_line_number")
    public Long getLastLineNumber() {
        return lastLineNumber;
    }

    @JsonProperty("last_line_number")
    public void setLastLineNumber(Long lastLineNumber) {
        this.lastLineNumber = lastLineNumber;
    }

    public GetJobLogsResults withLastLineNumber(Long lastLineNumber) {
        this.lastLineNumber = lastLineNumber;
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
        return ((((((("GetJobLogsResults"+" [lines=")+ lines)+", lastLineNumber=")+ lastLineNumber)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
