package us.kbase.narrativejobservice.db;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecLog {
    @JsonProperty("ujs_job_id")
    private String ujsJobId;
    @JsonProperty("original_line_count")
    private Integer originalLineCount;
    @JsonProperty("stored_line_count")
    private Integer storedLineCount;
    @JsonProperty("lines")
    private List<ExecLogLine> lines;
    
    @JsonProperty("ujs_job_id")
    public String getUjsJobId() {
        return ujsJobId;
    }
    
    @JsonProperty("ujs_job_id")
    public void setUjsJobId(String ujsJobId) {
        this.ujsJobId = ujsJobId;
    }
    
    @JsonProperty("original_line_count")
    public Integer getOriginalLineCount() {
        return originalLineCount;
    }
    
    @JsonProperty("original_line_count")
    public void setOriginalLineCount(Integer originalLineCount) {
        this.originalLineCount = originalLineCount;
    }
    
    @JsonProperty("stored_line_count")
    public Integer getStoredLineCount() {
        return storedLineCount;
    }
    
    @JsonProperty("stored_line_count")
    public void setStoredLineCount(Integer storedLineCount) {
        this.storedLineCount = storedLineCount;
    }
    
    @JsonProperty("lines")
    public List<ExecLogLine> getLines() {
        return lines;
    }
    
    @JsonProperty("lines")
    public void setLines(List<ExecLogLine> lines) {
        this.lines = lines;
    }
}
