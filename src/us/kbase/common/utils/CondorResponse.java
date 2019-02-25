package us.kbase.common.utils;
import java.util.List;

public class CondorResponse {
    public List<String> stdout;
    public List<String> stderr;
    public String jobID = "";
    public boolean success = false;

    CondorResponse(List<String> stdout, List<String> stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    CondorResponse(List<String> stdout, List<String> stderr, String jobID) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.jobID = jobID;
    }

    CondorResponse(List<String> stdout, List<String> stderr, boolean success) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.success = success;
    }
}