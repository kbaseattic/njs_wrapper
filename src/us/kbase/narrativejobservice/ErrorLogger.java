package us.kbase.narrativejobservice;

public interface ErrorLogger {
    public void logErr(String message);
    public void logErr(Throwable err);
}
