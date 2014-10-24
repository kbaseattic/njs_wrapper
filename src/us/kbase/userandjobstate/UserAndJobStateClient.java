package us.kbase.userandjobstate;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.Tuple14;
import us.kbase.common.service.Tuple2;
import us.kbase.common.service.Tuple5;
import us.kbase.common.service.Tuple7;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;

/**
 * <p>Original spec-file module name: UserAndJobState</p>
 * <pre>
 * Service for storing arbitrary key/object pairs on a per user per service basis
 * and storing job status so that a) long JSON RPC calls can report status and
 * UI elements can receive updates, and b) there's a centralized location for 
 * job status reporting.
 * There are two modes of operation for setting key values for a user: 
 * 1) no service authentication - an authorization token for a service is not 
 *         required, and any service with the user token can write to any other
 *         service's unauthed values for that user.
 * 2) service authentication required - the service must pass a Globus Online
 *         token that identifies the service in the argument list. Values can only be
 *         set by services with possession of a valid token. The service name 
 *         will be set to the username of the token.
 * The sets of key/value pairs for the two types of method calls are entirely
 * separate - for example, the workspace service could have a key called 'default'
 * that is writable by all other services (no auth) and the same key that was 
 * set with auth to which only the workspace service can write (or any other
 * service that has access to a workspace service account token, so keep your
 * service credentials safe).
 * Setting objects are limited to 640Kb.
 * All job writes require service authentication. No reads, either for key/value
 * pairs or jobs, require service authentication.
 * The service assumes other services are capable of simple math and does not
 * throw errors if a progress bar overflows.
 * Jobs are automatically deleted after 30 days.
 * Potential job process flows:
 * Asysnc:
 * UI calls service function which returns with job id
 * service call [spawns thread/subprocess to run job that] periodically updates
 *         the job status of the job id on the job status server
 * meanwhile, the UI periodically polls the job status server to get progress
 *         updates
 * service call finishes, completes job
 * UI pulls pointers to results from the job status server
 * Sync:
 * UI creates job, gets job id
 * UI starts thread that calls service, providing job id
 * service call runs, periodically updating the job status of the job id on the
 *         job status server
 * meanwhile, the UI periodically polls the job status server to get progress
 *         updates
 * service call finishes, completes job, returns results
 * UI thread joins
 * </pre>
 */
public class UserAndJobStateClient {
    private JsonClientCaller caller;
    private static URL DEFAULT_URL = null;
    static {
        try {
            DEFAULT_URL = new URL("https://kbase.us/services/userandjobstate/");
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Compile error in client - bad url compiled");
        }
    }

    /** Constructs a client with the default url and no user credentials.*/
    public UserAndJobStateClient() {
       caller = new JsonClientCaller(DEFAULT_URL);
    }


    /** Constructs a client with a custom URL and no user credentials.
     * @param url the URL of the service.
     */
    public UserAndJobStateClient(URL url) {
        caller = new JsonClientCaller(url);
    }
    /** Constructs a client with a custom URL.
     * @param url the URL of the service.
     * @param token the user's authorization token.
     * @throws UnauthorizedException if the token is not valid.
     * @throws IOException if an IOException occurs when checking the token's
     * validity.
     */
    public UserAndJobStateClient(URL url, AuthToken token) throws UnauthorizedException, IOException {
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
    public UserAndJobStateClient(URL url, String user, String password) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(url, user, password);
    }

    /** Constructs a client with the default URL.
     * @param token the user's authorization token.
     * @throws UnauthorizedException if the token is not valid.
     * @throws IOException if an IOException occurs when checking the token's
     * validity.
     */
    public UserAndJobStateClient(AuthToken token) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(DEFAULT_URL, token);
    }

    /** Constructs a client with the default URL.
     * @param user the user name.
     * @param password the password for the user name.
     * @throws UnauthorizedException if the credentials are not valid.
     * @throws IOException if an IOException occurs when checking the user's
     * credentials.
     */
    public UserAndJobStateClient(String user, String password) throws UnauthorizedException, IOException {
        caller = new JsonClientCaller(DEFAULT_URL, user, password);
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

    /** Get the token this client uses to communicate with the server.
     * @return the authorization token.
     */
    public AuthToken getToken() {
        return caller.getToken();
    }

    public void _setFileForNextRpcResponse(File f) {
        caller.setFileForNextRpcResponse(f);
    }

    /**
     * <p>Original spec-file function name: ver</p>
     * <pre>
     * Returns the version of the userandjobstate service.
     * </pre>
     * @return   parameter "ver" of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String ver() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("UserAndJobState.ver", args, retType, true, false);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: set_state</p>
     * <pre>
     * Set the state of a key for a service without service authentication.
     * </pre>
     * @param   service   instance of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   key   instance of String
     * @param   value   instance of unspecified object
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void setState(String service, String key, UObject value) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(key);
        args.add(value);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.set_state", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: set_state_auth</p>
     * <pre>
     * Set the state of a key for a service with service authentication.
     * </pre>
     * @param   token   instance of original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   key   instance of String
     * @param   value   instance of unspecified object
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void setStateAuth(String token, String key, UObject value) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(token);
        args.add(key);
        args.add(value);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.set_state_auth", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: get_state</p>
     * <pre>
     * Get the state of a key for a service.
     * </pre>
     * @param   service   instance of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   key   instance of String
     * @param   auth   instance of original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; original type "boolean" (A boolean. 0 = false, other = true.)
     * @return   parameter "value" of unspecified object
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public UObject getState(String service, String key, Long auth) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(key);
        args.add(auth);
        TypeReference<List<UObject>> retType = new TypeReference<List<UObject>>() {};
        List<UObject> res = caller.jsonrpcCall("UserAndJobState.get_state", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: has_state</p>
     * <pre>
     * Determine if a key exists for a service.
     * </pre>
     * @param   service   instance of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   key   instance of String
     * @param   auth   instance of original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; original type "boolean" (A boolean. 0 = false, other = true.)
     * @return   parameter "has_key" of original type "boolean" (A boolean. 0 = false, other = true.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Long hasState(String service, String key, Long auth) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(key);
        args.add(auth);
        TypeReference<List<Long>> retType = new TypeReference<List<Long>>() {};
        List<Long> res = caller.jsonrpcCall("UserAndJobState.has_state", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_has_state</p>
     * <pre>
     * Get the state of a key for a service, and do not throw an error if the
     * key doesn't exist. If the key doesn't exist, has_key will be false
     * and the key value will be null.
     * </pre>
     * @param   service   instance of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   key   instance of String
     * @param   auth   instance of original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; original type "boolean" (A boolean. 0 = false, other = true.)
     * @return   multiple set: (1) parameter "has_key" of original type "boolean" (A boolean. 0 = false, other = true.), (2) parameter "value" of unspecified object
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Tuple2<Long, UObject> getHasState(String service, String key, Long auth) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(key);
        args.add(auth);
        TypeReference<Tuple2<Long, UObject>> retType = new TypeReference<Tuple2<Long, UObject>>() {};
        Tuple2<Long, UObject> res = caller.jsonrpcCall("UserAndJobState.get_has_state", args, retType, true, true);
        return res;
    }

    /**
     * <p>Original spec-file function name: remove_state</p>
     * <pre>
     * Remove a key value pair without service authentication.
     * </pre>
     * @param   service   instance of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   key   instance of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void removeState(String service, String key) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(key);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.remove_state", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: remove_state_auth</p>
     * <pre>
     * Remove a key value pair with service authentication.
     * </pre>
     * @param   token   instance of original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   key   instance of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void removeStateAuth(String token, String key) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(token);
        args.add(key);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.remove_state_auth", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: list_state</p>
     * <pre>
     * List all keys.
     * </pre>
     * @param   service   instance of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   auth   instance of original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; original type "boolean" (A boolean. 0 = false, other = true.)
     * @return   parameter "keys" of list of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<String> listState(String service, Long auth) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(service);
        args.add(auth);
        TypeReference<List<List<String>>> retType = new TypeReference<List<List<String>>>() {};
        List<List<String>> res = caller.jsonrpcCall("UserAndJobState.list_state", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_state_services</p>
     * <pre>
     * List all state services.
     * </pre>
     * @param   auth   instance of original type "authed" (Specifies whether results returned should be from key/value pairs set with service authentication (true) or without (false).) &rarr; original type "boolean" (A boolean. 0 = false, other = true.)
     * @return   parameter "services" of list of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<String> listStateServices(Long auth) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(auth);
        TypeReference<List<List<String>>> retType = new TypeReference<List<List<String>>>() {};
        List<List<String>> res = caller.jsonrpcCall("UserAndJobState.list_state_services", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: create_job</p>
     * <pre>
     * Create a new job status report.
     * </pre>
     * @return   parameter "job" of original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String createJob() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("UserAndJobState.create_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: start_job</p>
     * <pre>
     * Start a job and specify the job parameters.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @param   token   instance of original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   instance of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   desc   instance of original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.)
     * @param   progress   instance of type {@link us.kbase.userandjobstate.InitProgress InitProgress}
     * @param   estComplete   instance of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time))
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void startJob(String job, String token, String status, String desc, InitProgress progress, String estComplete) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(token);
        args.add(status);
        args.add(desc);
        args.add(progress);
        args.add(estComplete);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.start_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: create_and_start_job</p>
     * <pre>
     * Create and start a job.
     * </pre>
     * @param   token   instance of original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   instance of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   desc   instance of original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.)
     * @param   progress   instance of type {@link us.kbase.userandjobstate.InitProgress InitProgress}
     * @param   estComplete   instance of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time))
     * @return   parameter "job" of original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String createAndStartJob(String token, String status, String desc, InitProgress progress, String estComplete) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(token);
        args.add(status);
        args.add(desc);
        args.add(progress);
        args.add(estComplete);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("UserAndJobState.create_and_start_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: update_job_progress</p>
     * <pre>
     * Update the status and progress for a job.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @param   token   instance of original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   instance of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   prog   instance of original type "progress" (The amount of progress the job has made since the last update. This will be summed to the total progress so far.)
     * @param   estComplete   instance of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time))
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void updateJobProgress(String job, String token, String status, Long prog, String estComplete) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(token);
        args.add(status);
        args.add(prog);
        args.add(estComplete);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.update_job_progress", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: update_job</p>
     * <pre>
     * Update the status for a job.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @param   token   instance of original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   instance of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   estComplete   instance of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time))
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void updateJob(String job, String token, String status, String estComplete) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(token);
        args.add(status);
        args.add(estComplete);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.update_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: get_job_description</p>
     * <pre>
     * Get the description of a job.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @return   multiple set: (1) parameter "service" of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.), (2) parameter "ptype" of original type "progress_type" (The type of progress that is being tracked. One of: 'none' - no numerical progress tracking 'task' - Task based tracking, e.g. 3/24 'percent' - percentage based tracking, e.g. 5/100%), (3) parameter "max" of original type "max_progress" (The maximum possible progress of a job.), (4) parameter "desc" of original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.), (5) parameter "started" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time))
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Tuple5<String, String, Long, String, String> getJobDescription(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<Tuple5<String, String, Long, String, String>> retType = new TypeReference<Tuple5<String, String, Long, String, String>>() {};
        Tuple5<String, String, Long, String, String> res = caller.jsonrpcCall("UserAndJobState.get_job_description", args, retType, true, true);
        return res;
    }

    /**
     * <p>Original spec-file function name: get_job_status</p>
     * <pre>
     * Get the status of a job.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @return   multiple set: (1) parameter "last_update" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)), (2) parameter "stage" of original type "job_stage" (A string that describes the stage of processing of the job. One of 'created', 'started', 'completed', or 'error'.), (3) parameter "status" of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.), (4) parameter "progress" of original type "total_progress" (The total progress of a job.), (5) parameter "est_complete" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)), (6) parameter "complete" of original type "boolean" (A boolean. 0 = false, other = true.), (7) parameter "error" of original type "boolean" (A boolean. 0 = false, other = true.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Tuple7<String, String, String, Long, String, Long, Long> getJobStatus(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<Tuple7<String, String, String, Long, String, Long, Long>> retType = new TypeReference<Tuple7<String, String, String, Long, String, Long, Long>>() {};
        Tuple7<String, String, String, Long, String, Long, Long> res = caller.jsonrpcCall("UserAndJobState.get_job_status", args, retType, true, true);
        return res;
    }

    /**
     * <p>Original spec-file function name: complete_job</p>
     * <pre>
     * Complete the job. After the job is completed, total_progress always
     * equals max_progress. If detailed_err is anything other than null,
     * the job is considered to have errored out.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @param   token   instance of original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   status   instance of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.)
     * @param   error   instance of original type "detailed_err" (Detailed information about a job error, such as a stacktrace, that will not fit in the job_status. No more than 100K characters.)
     * @param   res   instance of type {@link us.kbase.userandjobstate.Results Results}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void completeJob(String job, String token, String status, String error, Results res) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(token);
        args.add(status);
        args.add(error);
        args.add(res);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.complete_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: get_results</p>
     * <pre>
     * Get the job results.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @return   parameter "res" of type {@link us.kbase.userandjobstate.Results Results}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Results getResults(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<List<Results>> retType = new TypeReference<List<Results>>() {};
        List<Results> res = caller.jsonrpcCall("UserAndJobState.get_results", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_detailed_error</p>
     * <pre>
     * Get the detailed error message, if any
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @return   parameter "error" of original type "detailed_err" (Detailed information about a job error, such as a stacktrace, that will not fit in the job_status. No more than 100K characters.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String getDetailedError(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("UserAndJobState.get_detailed_error", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_job_info</p>
     * <pre>
     * Get information about a job.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @return   parameter "info" of original type "job_info" (Information about a job.) &rarr; tuple of size 14: parameter "job" of original type "job_id" (A job id.), parameter "service" of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.), parameter "stage" of original type "job_stage" (A string that describes the stage of processing of the job. One of 'created', 'started', 'completed', or 'error'.), parameter "started" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)), parameter "status" of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.), parameter "last_update" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)), parameter "prog" of original type "total_progress" (The total progress of a job.), parameter "max" of original type "max_progress" (The maximum possible progress of a job.), parameter "ptype" of original type "progress_type" (The type of progress that is being tracked. One of: 'none' - no numerical progress tracking 'task' - Task based tracking, e.g. 3/24 'percent' - percentage based tracking, e.g. 5/100%), parameter "est_complete" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)), parameter "complete" of original type "boolean" (A boolean. 0 = false, other = true.), parameter "error" of original type "boolean" (A boolean. 0 = false, other = true.), parameter "desc" of original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.), parameter "res" of type {@link us.kbase.userandjobstate.Results Results}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Tuple14<String, String, String, String, String, String, Long, Long, String, String, Long, Long, String, Results> getJobInfo(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<List<Tuple14<String, String, String, String, String, String, Long, Long, String, String, Long, Long, String, Results>>> retType = new TypeReference<List<Tuple14<String, String, String, String, String, String, Long, Long, String, String, Long, Long, String, Results>>>() {};
        List<Tuple14<String, String, String, String, String, String, Long, Long, String, String, Long, Long, String, Results>> res = caller.jsonrpcCall("UserAndJobState.get_job_info", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_jobs</p>
     * <pre>
     * List jobs. Leave 'services' empty or null to list jobs from all
     * services.
     * </pre>
     * @param   services   instance of list of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @param   filter   instance of original type "job_filter" (A string-based filter for listing jobs. If the string contains: 'R' - running jobs are returned. 'C' - completed jobs are returned. 'E' - jobs that errored out are returned. 'S' - shared jobs are returned. The string can contain any combination of these codes in any order. If the string contains none of the codes or is null, all self-owned jobs that have been started are returned. If only the S filter is present, all jobs that have been started are returned.)
     * @return   parameter "jobs" of list of original type "job_info" (Information about a job.) &rarr; tuple of size 14: parameter "job" of original type "job_id" (A job id.), parameter "service" of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.), parameter "stage" of original type "job_stage" (A string that describes the stage of processing of the job. One of 'created', 'started', 'completed', or 'error'.), parameter "started" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)), parameter "status" of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.), parameter "last_update" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)), parameter "prog" of original type "total_progress" (The total progress of a job.), parameter "max" of original type "max_progress" (The maximum possible progress of a job.), parameter "ptype" of original type "progress_type" (The type of progress that is being tracked. One of: 'none' - no numerical progress tracking 'task' - Task based tracking, e.g. 3/24 'percent' - percentage based tracking, e.g. 5/100%), parameter "est_complete" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)), parameter "complete" of original type "boolean" (A boolean. 0 = false, other = true.), parameter "error" of original type "boolean" (A boolean. 0 = false, other = true.), parameter "desc" of original type "job_description" (A job description string supplied by the reporting service. No more than 1000 characters.), parameter "res" of type {@link us.kbase.userandjobstate.Results Results}
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<Tuple14<String, String, String, String, String, String, Long, Long, String, String, Long, Long, String, Results>> listJobs(List<String> services, String filter) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(services);
        args.add(filter);
        TypeReference<List<List<Tuple14<String, String, String, String, String, String, Long, Long, String, String, Long, Long, String, Results>>>> retType = new TypeReference<List<List<Tuple14<String, String, String, String, String, String, Long, Long, String, String, Long, Long, String, Results>>>>() {};
        List<List<Tuple14<String, String, String, String, String, String, Long, Long, String, String, Long, Long, String, Results>>> res = caller.jsonrpcCall("UserAndJobState.list_jobs", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_job_services</p>
     * <pre>
     * List all job services.
     * </pre>
     * @return   parameter "services" of list of original type "service_name" (A service name. Alphanumerics and the underscore are allowed.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<String> listJobServices() throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<List<String>>> retType = new TypeReference<List<List<String>>>() {};
        List<List<String>> res = caller.jsonrpcCall("UserAndJobState.list_job_services", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: share_job</p>
     * <pre>
     * Share a job. Sharing a job to the same user twice or with the job owner
     * has no effect.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @param   users   instance of list of original type "username" (Login name of a KBase user account.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void shareJob(String job, List<String> users) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(users);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.share_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: unshare_job</p>
     * <pre>
     * Stop sharing a job. Removing sharing from a user that the job is not
     * shared with or the job owner has no effect.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @param   users   instance of list of original type "username" (Login name of a KBase user account.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void unshareJob(String job, List<String> users) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        args.add(users);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.unshare_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: get_job_owner</p>
     * <pre>
     * Get the owner of a job.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @return   parameter "owner" of original type "username" (Login name of a KBase user account.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String getJobOwner(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("UserAndJobState.get_job_owner", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_job_shared</p>
     * <pre>
     * Get the list of users with which a job is shared. Only the job owner
     * may access this method.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @return   parameter "users" of list of original type "username" (Login name of a KBase user account.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<String> getJobShared(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<List<List<String>>> retType = new TypeReference<List<List<String>>>() {};
        List<List<String>> res = caller.jsonrpcCall("UserAndJobState.get_job_shared", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: delete_job</p>
     * <pre>
     * Delete a job. Will fail if the job is not complete.
     * </pre>
     * @param   job   instance of original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void deleteJob(String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(job);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.delete_job", args, retType, false, true);
    }

    /**
     * <p>Original spec-file function name: force_delete_job</p>
     * <pre>
     * Force delete a job - will succeed unless the job has not been started.
     * In that case, the service must start the job and then delete it, since
     * a job is not "owned" by any service until it is started.
     * </pre>
     * @param   token   instance of original type "service_token" (A globus ID token that validates that the service really is said service.)
     * @param   job   instance of original type "job_id" (A job id.)
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void forceDeleteJob(String token, String job) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(token);
        args.add(job);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("UserAndJobState.force_delete_job", args, retType, false, true);
    }
}
