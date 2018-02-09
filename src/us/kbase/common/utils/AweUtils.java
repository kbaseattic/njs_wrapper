package us.kbase.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import us.kbase.auth.AuthToken;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AweUtils {
    
    @SuppressWarnings("unchecked")
    public static String runTask(String aweServerUrl, String pipeline, 
            String jobName, String args, String scriptName, AuthToken auth,
            String aweClientGroups, AuthToken adminAuth) throws JsonGenerationException, 
            JsonMappingException, IOException, AweResponseException {
        if (!aweServerUrl.endsWith("/"))
            aweServerUrl += "/";
        Map<String, Object> job = new LinkedHashMap<String, Object>();   // AwfTemplate
        Map<String, Object> info = new LinkedHashMap<String, Object>();  // AwfInfo
        info.put("pipeline", pipeline);
        info.put("name", jobName);
        info.put("project", "SDK");
        info.put("user", auth.getUserName());
        info.put("clientgroups", aweClientGroups);
        job.put("info", info);
        Map<String, Object> task = new LinkedHashMap<String, Object>();  // AwfTask
        Map<String, Object> cmd = new LinkedHashMap<String, Object>();   // AwfCmd
        cmd.put("args", args);
        cmd.put("name", scriptName);
        Map<String, Object> env = new LinkedHashMap<String, Object>();   // AwfEnviron
        env.put("public", new LinkedHashMap<String, Object>());
        Map<String, Object> priv = new LinkedHashMap<String, Object>();
        String token = auth.getToken();
        priv.put("KB_AUTH_TOKEN", token);
        if (adminAuth != null)
            priv.put("KB_ADMIN_AUTH_TOKEN", adminAuth.getToken());
        env.put("private", priv);
        cmd.put("environ", env);
        cmd.put("description", "");
        task.put("cmd", cmd);
        task.put("dependsOn", new ArrayList<String>());
        task.put("taskid", "0");
        task.put("skip", 0);
        task.put("totalwork", 1);
        job.put("tasks", Arrays.asList(task));
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(aweServerUrl + "job");
        httpPost.addHeader("Authorization", "OAuth " + token);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(buffer, job);
        builder.addBinaryBody("upload", buffer.toByteArray(),
                ContentType.APPLICATION_OCTET_STREAM, "tempjob.json");
        httpPost.setEntity(builder.build());
        Map<String, Object> respObj = parseAweResponse(httpClient.execute(httpPost));
        Map<String, Object> respData = (Map<String, Object>)respObj.get("data");
        if (respData == null)
            throw new IllegalStateException("AWE error: " + respObj.get("error"));
        String aweJobId = (String)respData.get("id");
        return aweJobId;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseAweResponse(
            HttpResponse response) throws IOException, ClientProtocolException,
            JsonParseException, JsonMappingException, AweResponseException {
        String postResponse = "" + EntityUtils.toString(response.getEntity());
        Map<String, Object> respObj;
        try {
            respObj = new ObjectMapper().readValue(postResponse, Map.class);
        } catch (Exception ex) {
            String respHead = postResponse.length() <= 1000 ? postResponse :
                    (postResponse.subSequence(0, 1000) + "...");
            throw new IllegalStateException("Error parsing JSON response of AWE server " +
            		"(" + ex.getMessage() + "). Here is the response head text: \n" +
            		respHead, ex);
        }
        int status = response.getStatusLine().getStatusCode();
        Integer jsonStatus = (Integer)respObj.get("status");
        Object errObj = respObj.get("error");
        if (status != 200 || jsonStatus != null && jsonStatus != 200 ||
                errObj != null) {
            if (jsonStatus == null)
                jsonStatus = status;
            String error = null;
            if (errObj != null) {
                if (errObj instanceof List) {
                    List<Object> errList = (List<Object>)errObj;
                    if (errList.size() == 1 && errList.get(0) instanceof String)
                        error = (String)errList.get(0);
                }
                if (error == null)
                    error = String.valueOf(errObj);
            }
            String reason = response.getStatusLine().getReasonPhrase();
            String fullMessage = "AWE error code " + jsonStatus + ": " +
                    (error == null ? reason : error);
            throw new AweResponseException(fullMessage, jsonStatus, reason, error);
        }
        return respObj;
    }

    public static Map<String, Object> getAweJobDescr(String aweServerUrl, 
            String aweJobId, AuthToken token) throws JsonParseException, 
            JsonMappingException, ClientProtocolException, IOException, 
            AweResponseException {
        if (!aweServerUrl.endsWith("/"))
            aweServerUrl += "/";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpReq = new HttpGet(aweServerUrl + "/job/" + aweJobId);
        httpReq.addHeader("Authorization", "OAuth " + token.getToken());
        return parseAweResponse(httpClient.execute(httpReq));
    }
    
    public static String getAweJobState(String aweServerUrl, String aweJobId,
            AuthToken token) throws JsonParseException, JsonMappingException, 
            ClientProtocolException, IOException, AweResponseException {
        Map<String, Object> aweJob = getAweJobDescr(aweServerUrl, aweJobId, token);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>)aweJob.get("data");
        return (String)data.get("state");
    }
    
    public static boolean checkAweJobIsNotDone(String aweState) {
        return aweState.equals("init") || aweState.equals("queued") || 
                aweState.equals("in-progress");
    }
    
    public static boolean checkAweJobIsDoneWithoutError(String aweState) {
        return aweState.equals("completed");
    }
    
    public static Map<String, Object> getAweJobPosition(String aweServerUrl, 
            String aweJobId, AuthToken token) throws JsonParseException, 
            JsonMappingException, ClientProtocolException, IOException, 
            AweResponseException {
        if (!aweServerUrl.endsWith("/"))
            aweServerUrl += "/";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpReq = new HttpGet(aweServerUrl + "/job/" + aweJobId + "?position");
        httpReq.addHeader("Authorization", "OAuth " + token.getToken());
        return parseAweResponse(httpClient.execute(httpReq));
    }
}
