package us.kbase.narrativejobservice.sdkjobs;

public interface ErrorLogger {
    public void logErr(String message);
    public void logErr(Throwable err);
}
