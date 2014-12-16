package us.kbase.common.taskqueue2;

public interface JobStatuses {
    public String createAndStartJob(String token, String status, String desc, String initProgressPtype, String estComplete) throws Exception;
    public void updateJob(String job, String token, String status, String estComplete) throws Exception;
    public void completeJob(String job, String token, String status, String error, String wsUrl, String outRef) throws Exception;
}
