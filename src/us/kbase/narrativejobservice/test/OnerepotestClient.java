package us.kbase.narrativejobservice.test;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JobState;
import us.kbase.common.service.JsonClientCaller;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.RpcContext;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;

/**
 * <p>Original spec-file module name: onerepotest</p>
 * <pre>
 * A KBase module: onerepotest
 * </pre>
 */
public class OnerepotestClient {
    private JsonClientCaller caller;
    private long asyncJobCheckTimeMs = 5000;
    private String asyncVersion = "dev";


    /** Constructs a client with a custom URL and no user credentials.
     * @param url the URL of the service.
     */
    public OnerepotestClient(URL url) {
        caller = new JsonClientCaller(url);
    }
    /** Constructs a client with a custom URL.
     * @param url the URL of the service.
     * @param token the user's authorization token.
     * @throws UnauthorizedException if the token is not valid.
     * @throws IOException if an IOException occurs when checking the token's
     * validity.
     */
    public OnerepotestClient(URL url, AuthToken token) throws UnauthorizedException, IOException {
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
    public OnerepotestClient(URL url, String user, String password) throws UnauthorizedException, IOException {
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

    public long getAsyncJobCheckTimeMs() {
        return this.asyncJobCheckTimeMs;
    }

    public void setAsyncJobCheckTimeMs(long newValue) {
        this.asyncJobCheckTimeMs = newValue;
    }

    public String getAsyncVersion() {
        return this.asyncVersion;
    }

    public void setAsyncVersion(String newValue) {
        this.asyncVersion = newValue;
    }

    /**
     * <p>Original spec-file function name: send_data</p>
     * <pre>
     * </pre>
     * @param   params   instance of unspecified object
     * @return   instance of unspecified object
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String sendDataAsync(UObject params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        if (asyncVersion != null) {
            if (jsonRpcContext == null || jsonRpcContext.length == 0 || jsonRpcContext[0] == null)
                jsonRpcContext = new RpcContext[] {new RpcContext()};
            jsonRpcContext[0].getAdditionalProperties().put("service_ver", asyncVersion);
        }
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("onerepotest._send_data_submit", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: send_data</p>
     * <pre>
     * </pre>
     * @param   params   instance of unspecified object
     * @return   instance of unspecified object
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public JobState<List<UObject>> sendDataCheck(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<JobState<List<UObject>>>> retType = new TypeReference<List<JobState<List<UObject>>>>() {};
        List<JobState<List<UObject>>> res = caller.jsonrpcCall("onerepotest._check_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: send_data</p>
     * <pre>
     * </pre>
     * @param   params   instance of unspecified object
     * @return   instance of unspecified object
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public UObject sendData(UObject params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        String jobId = sendDataAsync(params, jsonRpcContext);
        while (true) {
            if (Thread.currentThread().isInterrupted())
                throw new JsonClientException("Thread was interrupted");
            try { 
                Thread.sleep(this.asyncJobCheckTimeMs);
            } catch(Exception ex) {
                throw new JsonClientException("Thread was interrupted", ex);
            }
            JobState<List<UObject>> res = sendDataCheck(jobId);
            if (res.getFinished() != 0L)
                return res.getResult().get(0);
        }
    }

    /**
     * <p>Original spec-file function name: send_data</p>
     * <pre>
     * </pre>
     * @param   params   instance of unspecified object
     * @return   instance of unspecified object
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public UObject sendDataSync(UObject params, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(params);
        TypeReference<List<UObject>> retType = new TypeReference<List<UObject>>() {};
        List<UObject> res = caller.jsonrpcCall("onerepotest.send_data", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: print_lines</p>
     * <pre>
     * </pre>
     * @param   text   instance of String
     * @return   parameter "number_of_lines" of Long
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String printLinesAsync(String text, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        if (asyncVersion != null) {
            if (jsonRpcContext == null || jsonRpcContext.length == 0 || jsonRpcContext[0] == null)
                jsonRpcContext = new RpcContext[] {new RpcContext()};
            jsonRpcContext[0].getAdditionalProperties().put("service_ver", asyncVersion);
        }
        List<Object> args = new ArrayList<Object>();
        args.add(text);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("onerepotest._print_lines_submit", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: print_lines</p>
     * <pre>
     * </pre>
     * @param   text   instance of String
     * @return   parameter "number_of_lines" of Long
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public JobState<List<Long>> printLinesCheck(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<JobState<List<Long>>>> retType = new TypeReference<List<JobState<List<Long>>>>() {};
        List<JobState<List<Long>>> res = caller.jsonrpcCall("onerepotest._check_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: print_lines</p>
     * <pre>
     * </pre>
     * @param   text   instance of String
     * @return   parameter "number_of_lines" of Long
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Long printLines(String text, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        String jobId = printLinesAsync(text, jsonRpcContext);
        while (true) {
            if (Thread.currentThread().isInterrupted())
                throw new JsonClientException("Thread was interrupted");
            try { 
                Thread.sleep(this.asyncJobCheckTimeMs);
            } catch(Exception ex) {
                throw new JsonClientException("Thread was interrupted", ex);
            }
            JobState<List<Long>> res = printLinesCheck(jobId);
            if (res.getFinished() != 0L)
                return res.getResult().get(0);
        }
    }

    /**
     * <p>Original spec-file function name: print_lines</p>
     * <pre>
     * </pre>
     * @param   text   instance of String
     * @return   parameter "number_of_lines" of Long
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Long printLinesSync(String text, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(text);
        TypeReference<List<Long>> retType = new TypeReference<List<Long>>() {};
        List<Long> res = caller.jsonrpcCall("onerepotest.print_lines", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: generate_error</p>
     * <pre>
     * </pre>
     * @param   error   instance of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String generateErrorAsync(String error, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        if (asyncVersion != null) {
            if (jsonRpcContext == null || jsonRpcContext.length == 0 || jsonRpcContext[0] == null)
                jsonRpcContext = new RpcContext[] {new RpcContext()};
            jsonRpcContext[0].getAdditionalProperties().put("service_ver", asyncVersion);
        }
        List<Object> args = new ArrayList<Object>();
        args.add(error);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("onerepotest._generate_error_submit", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: generate_error</p>
     * <pre>
     * </pre>
     * @param   error   instance of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public JobState<Object> generateErrorCheck(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<JobState<Object>>> retType = new TypeReference<List<JobState<Object>>>() {};
        List<JobState<Object>> res = caller.jsonrpcCall("onerepotest._check_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: generate_error</p>
     * <pre>
     * </pre>
     * @param   error   instance of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void generateError(String error, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        String jobId = generateErrorAsync(error, jsonRpcContext);
        while (true) {
            if (Thread.currentThread().isInterrupted())
                throw new JsonClientException("Thread was interrupted");
            try { 
                Thread.sleep(this.asyncJobCheckTimeMs);
            } catch(Exception ex) {
                throw new JsonClientException("Thread was interrupted", ex);
            }
            JobState<Object> res = generateErrorCheck(jobId);
            if (res.getFinished() != 0L)
                return;
        }
    }

    /**
     * <p>Original spec-file function name: generate_error</p>
     * <pre>
     * </pre>
     * @param   error   instance of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public void generateErrorSync(String error, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(error);
        TypeReference<Object> retType = new TypeReference<Object>() {};
        caller.jsonrpcCall("onerepotest.generate_error", args, retType, false, true, jsonRpcContext);
    }

    /**
     * <p>Original spec-file function name: get_deploy_config</p>
     * <pre>
     * </pre>
     * @return   parameter "config" of mapping from String to String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String getDeployConfigAsync(RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        if (asyncVersion != null) {
            if (jsonRpcContext == null || jsonRpcContext.length == 0 || jsonRpcContext[0] == null)
                jsonRpcContext = new RpcContext[] {new RpcContext()};
            jsonRpcContext[0].getAdditionalProperties().put("service_ver", asyncVersion);
        }
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("onerepotest._get_deploy_config_submit", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_deploy_config</p>
     * <pre>
     * </pre>
     * @return   parameter "config" of mapping from String to String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public JobState<List<Map<String,String>>> getDeployConfigCheck(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<JobState<List<Map<String,String>>>>> retType = new TypeReference<List<JobState<List<Map<String,String>>>>>() {};
        List<JobState<List<Map<String,String>>>> res = caller.jsonrpcCall("onerepotest._check_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: get_deploy_config</p>
     * <pre>
     * </pre>
     * @return   parameter "config" of mapping from String to String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Map<String,String> getDeployConfig(RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        String jobId = getDeployConfigAsync(jsonRpcContext);
        while (true) {
            if (Thread.currentThread().isInterrupted())
                throw new JsonClientException("Thread was interrupted");
            try { 
                Thread.sleep(this.asyncJobCheckTimeMs);
            } catch(Exception ex) {
                throw new JsonClientException("Thread was interrupted", ex);
            }
            JobState<List<Map<String,String>>> res = getDeployConfigCheck(jobId);
            if (res.getFinished() != 0L)
                return res.getResult().get(0);
        }
    }

    /**
     * <p>Original spec-file function name: get_deploy_config</p>
     * <pre>
     * </pre>
     * @return   parameter "config" of mapping from String to String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public Map<String,String> getDeployConfigSync(RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        TypeReference<List<Map<String,String>>> retType = new TypeReference<List<Map<String,String>>>() {};
        List<Map<String,String>> res = caller.jsonrpcCall("onerepotest.get_deploy_config", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_ref_data</p>
     * <pre>
     * </pre>
     * @param   refDataPath   instance of String
     * @return   parameter "files" of list of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public String listRefDataAsync(String refDataPath, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        if (asyncVersion != null) {
            if (jsonRpcContext == null || jsonRpcContext.length == 0 || jsonRpcContext[0] == null)
                jsonRpcContext = new RpcContext[] {new RpcContext()};
            jsonRpcContext[0].getAdditionalProperties().put("service_ver", asyncVersion);
        }
        List<Object> args = new ArrayList<Object>();
        args.add(refDataPath);
        TypeReference<List<String>> retType = new TypeReference<List<String>>() {};
        List<String> res = caller.jsonrpcCall("onerepotest._list_ref_data_submit", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_ref_data</p>
     * <pre>
     * </pre>
     * @param   refDataPath   instance of String
     * @return   parameter "files" of list of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public JobState<List<List<String>>> listRefDataCheck(String jobId) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(jobId);
        TypeReference<List<JobState<List<List<String>>>>> retType = new TypeReference<List<JobState<List<List<String>>>>>() {};
        List<JobState<List<List<String>>>> res = caller.jsonrpcCall("onerepotest._check_job", args, retType, true, true);
        return res.get(0);
    }

    /**
     * <p>Original spec-file function name: list_ref_data</p>
     * <pre>
     * </pre>
     * @param   refDataPath   instance of String
     * @return   parameter "files" of list of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<String> listRefData(String refDataPath, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        String jobId = listRefDataAsync(refDataPath, jsonRpcContext);
        while (true) {
            if (Thread.currentThread().isInterrupted())
                throw new JsonClientException("Thread was interrupted");
            try { 
                Thread.sleep(this.asyncJobCheckTimeMs);
            } catch(Exception ex) {
                throw new JsonClientException("Thread was interrupted", ex);
            }
            JobState<List<List<String>>> res = listRefDataCheck(jobId);
            if (res.getFinished() != 0L)
                return res.getResult().get(0);
        }
    }

    /**
     * <p>Original spec-file function name: list_ref_data</p>
     * <pre>
     * </pre>
     * @param   refDataPath   instance of String
     * @return   parameter "files" of list of String
     * @throws IOException if an IO exception occurs
     * @throws JsonClientException if a JSON RPC exception occurs
     */
    public List<String> listRefDataSync(String refDataPath, RpcContext... jsonRpcContext) throws IOException, JsonClientException {
        List<Object> args = new ArrayList<Object>();
        args.add(refDataPath);
        TypeReference<List<List<String>>> retType = new TypeReference<List<List<String>>>() {};
        List<List<String>> res = caller.jsonrpcCall("onerepotest.list_ref_data", args, retType, true, true, jsonRpcContext);
        return res.get(0);
    }
}
