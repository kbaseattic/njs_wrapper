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

public class CondorUtils {

    @SuppressWarnings("unchecked")
    public static String submitToCondor(String condorUrl, 
            String jobName, String args, String scriptName, AuthToken auth,
            String clientGroups) throws JsonGenerationException, 
            JsonMappingException, IOException {
    	
    	// TODO: Submit job to Condor
    	
        Map<String, Object> job = new LinkedHashMap<String, Object>();
        Map<String, Object> info = new LinkedHashMap<String, Object>();    	
        Map<String, Object> cmd = new LinkedHashMap<String, Object>();

        info.put("name", jobName);
        info.put("project", "SDK");
        info.put("user", auth.getUserName());
        info.put("clientgroups", clientGroups);
        job.put("info", info);        
        cmd.put("name", scriptName);
        
        Map<String, Object> priv = new LinkedHashMap<String, Object>();
        Map<String, Object> env = new LinkedHashMap<String, Object>();
        String token = auth.getToken();
        priv.put("KB_AUTH_TOKEN", token);
        env.put("private", priv);
        
        
        
        // TODO: Replace below REST call (to Awe) with
        //    a Condor API client
        // --OR--
        //    Implement a system call architecture by compiling Condor binaries into njs
        //    To enable this as a trusted submit host to Condor
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
        HttpPost httpPost = new HttpPost( condorUrl + "job" );
        
        httpPost.addHeader("Authorization", "OAuth " + token);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(buffer, job);
        builder.addBinaryBody("upload", buffer.toByteArray(),
                ContentType.APPLICATION_OCTET_STREAM, "tempjob.json");
        httpPost.setEntity(builder.build());
        // Map<String, Object> respObj = parseAweResponse(httpClient.execute(httpPost));
        // Map<String, Object> respData = (Map<String, Object>)respObj.get("data");
        // if (respData == null) throw new IllegalStateException("AWE error: " + respObj.get("error"));
        // String aweJobId = (String)respData.get("id");        

        return jobName;    	
    }
    
}
