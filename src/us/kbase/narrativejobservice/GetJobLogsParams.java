
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
 * <p>Original spec-file type: GetJobLogsParams</p>
 * <pre>
 * skip_lines - optional parameter, number of lines to skip (in case they were 
 *     already loaded before).
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "job_id",
    "skip_lines"
})
public class GetJobLogsParams {

    @JsonProperty("job_id")
    private String jobId;
    @JsonProperty("skip_lines")
    private Long skipLines;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("job_id")
    public String getJobId() {
        return jobId;
    }

    @JsonProperty("job_id")
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public GetJobLogsParams withJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    @JsonProperty("skip_lines")
    public Long getSkipLines() {
        return skipLines;
    }

    @JsonProperty("skip_lines")
    public void setSkipLines(Long skipLines) {
        this.skipLines = skipLines;
    }

    public GetJobLogsParams withSkipLines(Long skipLines) {
        this.skipLines = skipLines;
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
        return ((((((("GetJobLogsParams"+" [jobId=")+ jobId)+", skipLines=")+ skipLines)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
