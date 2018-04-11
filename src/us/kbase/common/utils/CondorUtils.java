package us.kbase.common.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.commons.io.IOUtils;




import us.kbase.auth.AuthToken;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.lang.NumberFormatException;
import us.kbase.common.utils.CondorResponse;


	
public class CondorUtils
{



    public static CondorResponse runProcess(String[] condorCommand) throws IOException {
        /**
         * Run the condor command and return
         * @param condorCommand command to run
         * @return CondorResponse with STDIN and STDOUT
         */
        //TODO DELETE PRINT STATEMENT
        System.out.println("Running command: [" + String.join(" ", condorCommand) + "]");
        final Process process =  Runtime.getRuntime().exec(condorCommand);
        try {
            process.waitFor();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<String> stdOutMessage = IOUtils.readLines(process.getInputStream(), "UTF-8");
        List<String> stdErrMessage = IOUtils.readLines(process.getErrorStream(), "UTF-8");

        if (process.exitValue() != 0) {
            System.err.println("STDOUT:");
            for (String s : stdOutMessage){
                System.err.println(s);
            }
            System.out.println("STDERR:");
            for (String s : stdOutMessage){
                System.err.println(stdErrMessage);
            }
            throw new IOException("Error running condorCommand:" + String.join(" ", condorCommand));
        }
        return new CondorResponse(stdOutMessage, stdErrMessage);
    }

    public static String submitToCondorCLI ( String ujsJobId, AuthToken token ) throws IOException {
        /**
         * Call condor_submit with the ujsJobId as batch job name
         * @param jobID ujsJobId to name the batch job with
         * @param token token to place into the shell script
         * @return String condor job id
         */
        //TODO USE TOKEN IN CONDOR_SUBMIT
        //TODO USE CLIENTGROUPS
        //TODO See if there is anything else to pass
        String[] cmdScript = new String[]{ "/kb/deployment/misc/condor_submit.sh", ujsJobId };
        String ret = runProcess(cmdScript).stdout.get(0);
        return ret;
    }


    public static String condorQ(String ujsJobId, String attribute) throws IOException {
        /**
         * Call condor_q with the ujsJobId a string target to filter condor_q
         * @param jobID ujsJobId to get job JobPrio for
         * @param attribute attribute to search condorQ for
         * @return String condor job attribute or NULL
         */
        String[] cmdScript = new String[]{"/kb/deployment/misc/condor_q.sh", ujsJobId, attribute};
        String result = String.join("\n", runProcess(cmdScript).stdout);
        // convert JSON string to Map There has to be a better way than this
        if (result.contains(attribute)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> myObjects =
                        mapper.readValue(result, new TypeReference<List<Map<String, Object>>>() {
                        });
                return myObjects.get(0).get(attribute).toString();
            } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
                return null;
            }
        }
        else{
            return null;
        }
    }

    public static String getJobState(String ujsJobId) throws Exception{
        /**
         * Get job state from condor_q with the LastJobStatus param
         * @param jobID ujsJobId to get job state for
         * @return String  condor job state or NULL
         */
        return condorQ(ujsJobId, "LastJobStatus");
    }
    public static String getJobPriority(String ujsJobId) throws Exception{
        /**
         * Get job priority from condor_q with the JobPrio param
         * @param jobID ujsJobId to get job JobPrio for
         * @return String  condor job priority or NULL
         */
        return condorQ(ujsJobId, "JobPrio");
    }




	
} // class CondorUtils
