package us.kbase.common.utils;
import java.util.List;

public class CondorResponse {
    public List<String> stdout;
    public List<String> stderr;
    public String jobID = "";

    CondorResponse(List<String> stdout, List<String> stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    CondorResponse(List<String> stdout, List<String> stderr, String jobID) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.jobID = jobID;
    }
}