package us.kbase.narrativejobservice;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UnauthorizedException;

/**
 * <p>Original spec-file module name: NarrativeJobService</p>
 * <pre>
 * </pre>
 */
public class NarrativeJobServiceClient {
    private JsonClientCaller caller;


    /** Constructs a client with a custom URL and no user credentials.
     * @param url the URL of the service.
     */
    public NarrativeJobServiceClient(URL url) {
        caller = new JsonClientCaller(url);
    }
    /** Constructs a client with a custom URL.
     * @param url the URL of the service.
     * @param token the user's authorization token.
     * @throws UnauthorizedException if the token is not valid.
     * @throws IOException if an IOException occurs when checking the token's
     * validity.
     */
    public NarrativeJobServiceClient(URL url, AuthToken token) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(url, token);
    }

    /** Constructs a client with a custom URL.
     * @param url the URL of the service.
     * @param user the user name.
     * @param password the password for the user name.
     * @throws UnauthorizedException if the credentials are not valid.
     * @throws IOException if an IOException occurs when checking the user's
     * credentials.
     */
    public NarrativeJobServiceClient(URL url, String user, String password) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(url, user, password);
    }

    /** Get the token this client uses to communicate with the server.
     * @return the authorization token.
     */
    public AuthToken getToken() {
        return caller.getToken();
    }

    /** Get the URL of the service with which this client communicates.
     * @return the service URL.
     */
    public URL getURL() {
        return caller.getURL();
    }

    /** Set the timeout between establishing a connection to a server and
     * receiving a response. A value of zero or null implies no timeout.
     * @param milliseconds the milliseconds to wait before timing out when
     * attempting to read from a server.
     */
    public void setConnectionReadTimeOut(Integer milliseconds) {
        this.caller.setConnectionReadTimeOut(milliseconds);
    }

    /** Check if this client allows insecure http (vs https) connections.
     * @return true if insecure connections are allowed.
     */
    public boolean isInsecureHttpConnectionAllowed() {
        return caller.isInsecureHttpConnectionAllowed();
    }

    /** Deprecated. Use isInsecureHttpConnectionAllowed().
     * @deprecated
     */
    public boolean isAuthAllowedForHttp() {
        return caller.isAuthAllowedForHttp();
    }

    /** Set whether insecure http (vs https) connections should be allowed by
     * this client.
     * @param allowed true to allow insecure connections. Default false
     */
    public void setIsInsecureHttpConnectionAllowed(boolean allowed) {
        caller.setInsecureHttpConnectionAllowed(allowed);
    }

    /** Deprecated. Use setInsecureHttpConnectionAllowed().
     * @deprecated
     */
    public void setAuthAllowedForHttp(boolean isAuthAllowedForHttp) {
        caller.setAuthAllowedForHttp(isAuthAllowedForHttp);
    }

    /** Set whether all SSL certificates, including self-signed certificates,
     * should be trusted.
     * @param trustAll true to trust all certificates. Default false.
     */
    public void setAllSSLCertificatesTrusted(final boolean trustAll) {
        caller.setAllSSLCertificatesTrusted(trustAll);
    }
    
    /** Check if this client trusts all SSL certificates, including
     * self-signed certificates.
     * @return true if all certificates are trusted.
     */
    public boolean isAllSSLCertificatesTrusted() {
        return caller.isAllSSLCertificatesTrusted();
    }
    /** Sets streaming mode on. In this case, the data will be streamed to
     * the server in chunks as it is read from disk rather than buffered in
     * memory. Many servers are not compatible with this feature.
     * @param streamRequest true to set streaming mode on, false otherwise.
     */
    public void setStreamingModeOn(boolean streamRequest) {
        caller.setStreamingModeOn(streamRequest);
    }

    /** Returns true if streaming mode is on.
     * @return true if streaming mode is on.
     */
    public boolean isStreamingModeOn() {
        return caller.isStreamingModeOn();
    }

    public void _setFileForNextRpcResponse(File f) {
        caller.setFileForNextRpcResponse(f);
    }

    /**
     * <p>Original spec-file function name: run_app</p>
     * <pre>
     * </pre>
     * @param   app   instance of type {@link us.kbase.narrativejobservice.App App} (original type "app")
     * @return   instance of type {@link us.kbase.narrativejobservice.AppState AppState} (original type "app_state")
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public AppState runApp(App app) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(app);
        TypeReference<List<AppState>> retType = new TypeReference<List<AppState>>() {};
        List<AppState> res = caller.jsonrpcCall("NarrativeJobService.run_app", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: check_app_state</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   instance of type {@link us.kbase.narrativejobservice.AppState AppState} (original type "app_state")
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public AppState checkAppState(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<AppState>> retType = new TypeReference<List<AppState>>() {};
        List<AppState> res = caller.jsonrpcCall("NarrativeJobService.check_app_state", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: suspend_app</p>
     * <pre>
     * status - 'success' or 'failure' of action
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   parameter "status" of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String suspendApp(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("NarrativeJobService.suspend_app", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: resume_app</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   parameter "status" of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String resumeApp(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("NarrativeJobService.resume_app", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: delete_app</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   parameter "status" of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String deleteApp(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("NarrativeJobService.delete_app", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_config</p>
     * <pre>
     * </pre>
     * @return   instance of mapping from String to String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Map<String,String> listConfig() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<Map<String,String>>> retType = new TypeReference<List<Map<String,String>>>() {};
        List<Map<String,String>> res = caller.jsonrpcCall("NarrativeJobService.list_config", args, retType, true, false);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: ver</p>
     * <pre>
     * Returns the current running version of the NarrativeJobService.
     * </pre>
     * @return   instance of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String ver() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("NarrativeJobService.ver", args, retType, true, false);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: status</p>
     * <pre>
     * Simply check the status of this service to see queue details
     * </pre>
     * @return   instance of type {@link us.kbase.narrativejobservice.Status Status}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Status status() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<Status>> retType = new TypeReference<List<Status>>() {};
        List<Status> res = caller.jsonrpcCall("NarrativeJobService.status", args, retType, true, false);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_running_apps</p>
     * <pre>
     * </pre>
     * @return   instance of list of type {@link us.kbase.narrativejobservice.AppState AppState} (original type "app_state")
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<AppState> listRunningApps() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<List<AppState>>> retType = new TypeReference<List<List<AppState>>>() {};
        List<List<AppState>> res = caller.jsonrpcCall("NarrativeJobService.list_running_apps", args, retType, true, false);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: run_job</p>
     * <pre>
     * Start a new job (long running method of service registered in ServiceRegistery).
     * Such job runs Docker image for this service in script mode.
     * </pre>
     * @param   params   instance of type {@link us.kbase.narrativejobservice.RunJobParams RunJobParams}
     * @return   parameter "job_id" of original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String runJob(RunJobParams params) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("NarrativeJobService.run_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: check_job</p>
     * <pre>
     * Check if a job is finished and get results/error
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   parameter "job_state" of type {@link us.kbase.narrativejobservice.JobState JobState}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public JobState checkJob(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<JobState>> retType = new TypeReference<List<JobState>>() {};
        List<JobState> res = caller.jsonrpcCall("NarrativeJobService.check_job", args, retType, true, true);
        return res.get(0);
    }
}
