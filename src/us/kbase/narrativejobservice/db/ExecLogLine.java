package us.kbase.narrativejobservice.db;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecLogLine {
    @JsonProperty("line_pos")
    private Integer linePos;
    @JsonProperty("line")
    private String line;
    @JsonProperty("is_error")
    private Boolean isError;
    
    @JsonProperty("line_pos")
    public Integer getLinePos() {
        return linePos;
    }
    
    @JsonProperty("line_pos")
    public void setLinePos(Integer linePos) {
        this.linePos = linePos;
    }
    
    @JsonProperty("line")
    public String getLine() {
        return line;
    }
    
    @JsonProperty("line")
    public void setLine(String line) {
        this.line = line;
    }
    
    @JsonProperty("is_error")
    public Boolean getIsError() {
        return isError;
    }
    
    @JsonProperty("is_error")
    public void setIsError(Boolean isError) {
        this.isError = isError;
    }
}
