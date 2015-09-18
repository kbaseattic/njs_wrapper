package us.kbase.common.utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AweUtils {
    
    @SuppressWarnings("unchecked")
    public static String runTask(String aweServerUrl, String pipeline, 
            String jobName, String args, String scriptName, String token) throws Exception {
        if (!aweServerUrl.endsWith("/"))
            aweServerUrl += "/";
        Map<String, Object> job = new LinkedHashMap<String, Object>();   // AwfTemplate
        Map<String, Object> info = new LinkedHashMap<String, Object>();  // AwfInfo
        info.put("pipeline", pipeline);
        info.put("name", jobName);
        info.put("project", "default");
        info.put("user", "default");
        info.put("clientgroups", "");
        job.put("info", info);
        Map<String, Object> task = new LinkedHashMap<String, Object>();  // AwfTask
        Map<String, Object> cmd = new LinkedHashMap<String, Object>();   // AwfCmd
        cmd.put("args", args);
        cmd.put("name", scriptName);
        Map<String, Object> env = new LinkedHashMap<String, Object>();   // AwfEnviron
        if (token != null) {
            env.put("public", new LinkedHashMap<String, Object>());
            Map<String, Object> priv = new LinkedHashMap<String, Object>();
            priv.put("KB_AUTH_TOKEN", token);
            env.put("private", priv);
        }
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
        HttpResponse response = httpClient.execute(httpPost);
        String postResponse = EntityUtils.toString(response.getEntity());
        Map<String, Object> respObj = mapper.readValue(postResponse, Map.class);
        Map<String, Object> respData = (Map<String, Object>)respObj.get("data");
        if (respData == null)
            throw new IllegalStateException("AWE error: " + respObj.get("error"));
        String aweJobId = (String)respData.get("id");
        return aweJobId;
    }

    public static Map<String, Object> getAweJobDescr(String aweServerUrl, 
            String aweJobId, String token) throws Exception {
        if (!aweServerUrl.endsWith("/"))
            aweServerUrl += "/";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpReq = new HttpGet(aweServerUrl + "/job/" + aweJobId);
        httpReq.addHeader("Authorization", "OAuth " + token);
        HttpResponse response = httpClient.execute(httpReq);
        String respString = EntityUtils.toString(response.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> ret = mapper.readValue(respString, Map.class);
        return ret;
    }
    
    public static String getAweJobState(String aweServerUrl, String aweJobId,
            String token) throws Exception {
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
}
