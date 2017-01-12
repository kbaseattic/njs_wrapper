package us.kbase.narrativejobservice;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.RpcContext;
import us.kbase.common.service.Tuple2;
import us.kbase.common.service.UnauthorizedException;

/**
 * <p>Original spec-file module name: NarrativeJobService</p>
 * <pre>
 * </pre>
 */
public class NarrativeJobServiceClient {
    private JsonClientCaller caller;
    private String serviceVersion = null;
    private static URL DEFAULT_URL = null;
    static {
        try {
            DEFAULT_URL = new URL("https://kbase.us/services/njs_wrapper/");
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Compile error in client - bad url compiled");
        }
    }

    /** Constructs a client with the default url and no user credentials.*/
    public NarrativeJobServiceClient() {
       caller = new JsonClientCaller(DEFAULT_URL);
    }


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

    /** Constructs a client with a custom URL
     * and a custom authorization service URL.
     * @param url the URL of the service.
     * @param user the user name.
     * @param password the password for the user name.
     * @param auth the URL of the authorization server.
     * @throws UnauthorizedException if the credentials are not valid.
     * @throws IOException if an IOException occurs when checking the user's
     * credentials.
     */
    public NarrativeJobServiceClient(URL url, String user, String password, URL auth) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(url, user, password, auth);
    }

    /** Constructs a client with the default URL.
     * @param token the user's authorization token.
     * @throws UnauthorizedException if the token is not valid.
     * @throws IOException if an IOException occurs when checking the token's
     * validity.
     */
    public NarrativeJobServiceClient(AuthToken token) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(DEFAULT_URL, token);
    }

    /** Constructs a client with the default URL.
     * @param user the user name.
     * @param password the password for the user name.
     * @throws UnauthorizedException if the credentials are not valid.
     * @throws IOException if an IOException occurs when checking the user's
     * credentials.
     */
    public NarrativeJobServiceClient(String user, String password) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(DEFAULT_URL, user, password);
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

    /** Deprecated. Use setIsInsecureHttpConnectionAllowed().
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

    public String getServiceVersion() {
        return this.serviceVersion;
    }

    public void setServiceVersion(String newValue) {
        this.serviceVersion = newValue;
    }

    /**
     * <p>Original spec-file function name: list_config</p>
     * <pre>
     * </pre>
     * @return   instance of mapping from String to String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Map<String,String> listConfig(RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<Map<String,String>>> retType = new TypeReference<List<Map<String,String>>>() {};
        List<Map<String,String>> res = caller.jsonrpcCall("NarrativeJobService.list_config", args, retType, true, false, jsonRpcContext, this.serviceVersion);
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
    public String ver(RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("NarrativeJobService.ver", args, retType, true, false, jsonRpcContext, this.serviceVersion);
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
    public Status status(RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<Status>> retType = new TypeReference<List<Status>>() {};
        List<Status> res = caller.jsonrpcCall("NarrativeJobService.status", args, retType, true, false, jsonRpcContext, this.serviceVersion);
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
    public String runJob(RunJobParams params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("NarrativeJobService.run_job", args, retType, true, true, jsonRpcContext, this.serviceVersion);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_job_params</p>
     * <pre>
     * Get job params necessary for job execution
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @return   multiple set: (1) parameter "params" of type {@link us.kbase.narrativejobservice.RunJobParams RunJobParams}, (2) parameter "config" of mapping from String to String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Tuple2<RunJobParams, Map<String,String>> getJobParams(String jobId, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<Tuple2<RunJobParams, Map<String,String>>> retType = new TypeReference<Tuple2<RunJobParams, Map<String,String>>>() {};
        Tuple2<RunJobParams, Map<String,String>> res = caller.jsonrpcCall("NarrativeJobService.get_job_params", args, retType, true, true, jsonRpcContext, this.serviceVersion);
        return res;
    }

    /**
     * <p>Original spec-file function name: update_job</p>
     * <pre>
     * </pre>
     * @param   params   instance of type {@link us.kbase.narrativejobservice.UpdateJobParams UpdateJobParams}
     * @return   instance of type {@link us.kbase.narrativejobservice.UpdateJobResults UpdateJobResults}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public UpdateJobResults updateJob(UpdateJobParams params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<List<UpdateJobResults>> retType = new TypeReference<List<UpdateJobResults>>() {};
        List<UpdateJobResults> res = caller.jsonrpcCall("NarrativeJobService.update_job", args, retType, true, true, jsonRpcContext, this.serviceVersion);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: add_job_logs</p>
     * <pre>
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @param   lines   instance of list of type {@link us.kbase.narrativejobservice.LogLine LogLine}
     * @return   parameter "line_number" of Long
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Long addJobLogs(String jobId, List<LogLine> lines, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        args.add(lines);
        TypeReference<List<Long>> retType = new TypeReference<List<Long>>() {};
        List<Long> res = caller.jsonrpcCall("NarrativeJobService.add_job_logs", args, retType, true, true, jsonRpcContext, this.serviceVersion);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_job_logs</p>
     * <pre>
     * </pre>
     * @param   params   instance of type {@link us.kbase.narrativejobservice.GetJobLogsParams GetJobLogsParams}
     * @return   instance of type {@link us.kbase.narrativejobservice.GetJobLogsResults GetJobLogsResults}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public GetJobLogsResults getJobLogs(GetJobLogsParams params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<List<GetJobLogsResults>> retType = new TypeReference<List<GetJobLogsResults>>() {};
        List<GetJobLogsResults> res = caller.jsonrpcCall("NarrativeJobService.get_job_logs", args, retType, true, true, jsonRpcContext, this.serviceVersion);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: finish_job</p>
     * <pre>
     * Register results of already started job
     * </pre>
     * @param   jobId   instance of original type "job_id" (A job id.)
     * @param   params   instance of type {@link us.kbase.narrativejobservice.FinishJobParams FinishJobParams}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void finishJob(String jobId, FinishJobParams params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        args.add(params);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("NarrativeJobService.finish_job", args, retType, false, true, jsonRpcContext, this.serviceVersion);
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
    public JobState checkJob(String jobId, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<JobState>> retType = new TypeReference<List<JobState>>() {};
        List<JobState> res = caller.jsonrpcCall("NarrativeJobService.check_job", args, retType, true, true, jsonRpcContext, this.serviceVersion);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: check_jobs</p>
     * <pre>
     * </pre>
     * @param   params   instance of type {@link us.kbase.narrativejobservice.CheckJobsParams CheckJobsParams}
     * @return   instance of type {@link us.kbase.narrativejobservice.CheckJobsResults CheckJobsResults}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public CheckJobsResults checkJobs(CheckJobsParams params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<List<CheckJobsResults>> retType = new TypeReference<List<CheckJobsResults>>() {};
        List<CheckJobsResults> res = caller.jsonrpcCall("NarrativeJobService.check_jobs", args, retType, true, true, jsonRpcContext, this.serviceVersion);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: cancel_job</p>
     * <pre>
     * </pre>
     * @param   params   instance of type {@link us.kbase.narrativejobservice.CancelJobParams CancelJobParams}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void cancelJob(CancelJobParams params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("NarrativeJobService.cancel_job", args, retType, false, true, jsonRpcContext, this.serviceVersion);
    }

    /**
     * <p>Original spec-file function name: check_job_canceled</p>
     * <pre>
     * Check whether a job has been canceled. This method is lightweight compared to check_job.
     * </pre>
     * @param   params   instance of type {@link us.kbase.narrativejobservice.CancelJobParams CancelJobParams}
     * @return   parameter "result" of type {@link us.kbase.narrativejobservice.CheckJobCanceledResult CheckJobCanceledResult}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public CheckJobCanceledResult checkJobCanceled(CancelJobParams params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<List<CheckJobCanceledResult>> retType = new TypeReference<List<CheckJobCanceledResult>>() {};
        List<CheckJobCanceledResult> res = caller.jsonrpcCall("NarrativeJobService.check_job_canceled", args, retType, true, true, jsonRpcContext, this.serviceVersion);
        return res.get(0);
    }
}
