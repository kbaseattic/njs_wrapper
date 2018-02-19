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

import org.apache.axis.*;

import condor.*;

import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;
	
	
public class CondorUtils
{

	private static ClassAdStructAttr[] buildJobAd(String owner, String jobFileLocation, int clusterId, int jobId)
	{
		String jobOutputFileLocation = jobFileLocation + ".job.out";
		String jobLogFileLocation = jobFileLocation + ".job.log";
		String stdOutLocation = jobFileLocation + ".stdout";
		String stdErrLocation = jobFileLocation + ".stderr";
		String dagmanLockFile = jobFileLocation + ".lock";
		String workingDirectory = jobFileLocation.substring(0, jobFileLocation.lastIndexOf("/"));
		
		ClassAdStructAttr[] jobAd =
		{
			createStringAttribute("Owner", owner), // Need to insert kbase username@realm here
			createStringAttribute("Iwd", workingDirectory), // Awe creates a working directory per job (uuid) we may need to generate one, or use the job id.  Not sure if condor will create this directory.  If not, we may need to handle working directory creation in the async runner script. 
			createIntAttribute("JobUniverse", 5), // Vanilla Universe
			createStringAttribute("Cmd", "run_async_srv_method.sh"),
			createIntAttribute("JobStatus", 1), // Idle
			createStringAttribute("Env",
			  "_CONDOR_MAX_LOG=0;" +
			  "_CONDOR_LOG=" + jobOutputFileLocation), //leaving in for example setting env var - not needed for kbase
			createIntAttribute("JobNotification", 0), // Never
			createStringAttribute("UserLog", jobLogFileLocation),
			createStringAttribute("RemoveKillSig", "SIGUSR1"),
			createStringAttribute("Out", stdOutLocation),
			createStringAttribute("Err", stdErrLocation),
			createStringAttribute("ShouldTransferFiles", "NO"), // Using shared FS
			
			// XXX: Job id 62.0 has no Owner attribute.  Removing.
			// TODO: Fix Requirements expression; like:
			//   	createExpressionAttribute("Requirements", "TARGET.Owner=='amikaili@???'"),
			// http://research.cs.wisc.edu/htcondor/manual/v7.6/4_1Condor_s_ClassAd.html#sec:classad-reference
			// Owner has "Policy" semantics and configuration connotation
			// Is used to map jobs to machines
			// http://research.cs.wisc.edu/htcondor/manual/v7.6/3_5Policy_Configuration.html
			// 
			createExpressionAttribute("Requirements", "TRUE"),
			
			createExpressionAttribute("OnExitRemove",
			      "(ExitSignal =?= 11 || " +
			      " (ExitCode =!= UNDEFINED && " +
			      "  ExitCode >=0 && ExitCode <= 2))"),
			createStringAttribute("Arguments",
			  "-f -l . -Debug 3 "), // also leaving - we can modify for kbase arguments
			createIntAttribute("ClusterId", clusterId),
			createIntAttribute("ProcId", jobId)
		};
		
		return jobAd;
	}

	private static ClassAdStructAttr createStringAttribute(String name, String value)
	{
		return createAttribute(name, value, ClassAdAttrType.value3);
	}

	private static ClassAdStructAttr createIntAttribute(String name, int value)
	{
		return createAttribute(name,
		String.valueOf(value),
		ClassAdAttrType.value1);
	}

	private static ClassAdStructAttr createExpressionAttribute(String name, String value)
	{
		return createAttribute(name, value, ClassAdAttrType.value4);
	}

	private static ClassAdStructAttr createAttribute(String name, String value, ClassAdAttrType type)
	{
		ClassAdStructAttr attribute = new ClassAdStructAttr();
		attribute.setName(name);
		attribute.setValue(value);
		attribute.setType(type);
		return attribute;
	}



	public static int submitToCondor( String condorUrl, String owner, String jobFileLocation,
	        // String jobName, String args, String scriptName, AuthToken auth,
	        String clientGroups) throws MalformedURLException, RemoteException, ServiceException {
	
		URL scheddLocation = new URL( condorUrl );
		
		// Get a handle on a schedd we can make SOAP call on.
		CondorScheddLocator scheddLocator = new CondorScheddLocator();
		CondorScheddPortType schedd = scheddLocator.getcondorSchedd(scheddLocation);	
	
		// Begin a transaction, allow for 60 seconds between calls
		TransactionAndStatus transactionAndStatus = schedd.beginTransaction(60);
		Transaction transaction = transactionAndStatus.getTransaction();
		
		// Get a new cluster for the job.
		IntAndStatus clusterIdAndStatus = schedd.newCluster(transaction);
		int clusterId = clusterIdAndStatus.getInteger();	
		
		// Get a new Job ID (aka a ProcId) for the Job.
		IntAndStatus jobIdAndStatus = schedd.newJob(transaction, clusterId);
		int jobId = jobIdAndStatus.getInteger();
		
		// Build the Job's ClassAd.
		ClassAdStructAttr[] jobAd = buildJobAd(owner, jobFileLocation, clusterId, jobId);	
		
		// Submit the Job's ClassAd.
		schedd.submit(transaction, clusterId, jobId, jobAd);	
		
		// Commit the transaction.
		schedd.commitTransaction(transaction);
	
		// Ask the Schedd to kick off the Job immediately.
		schedd.requestReschedule();
		
	    return jobId;    	
	}



	public static void main(String[] arguments)
	throws MalformedURLException, RemoteException, ServiceException
	{
		URL scheddLocation = new URL(arguments[0]);
		String owner = arguments[1];
		String jobFileLocation = arguments[2];
		
		// Get a handle on a schedd we can make SOAP call on.
		CondorScheddLocator scheddLocator = new CondorScheddLocator();
		CondorScheddPortType schedd =
		scheddLocator.getcondorSchedd(scheddLocation);
		
		// Begin a transaction, allow for 60 seconds between calls
		TransactionAndStatus transactionAndStatus = schedd.beginTransaction(60);
		
		Transaction transaction = transactionAndStatus.getTransaction();
		// Get a new cluster for the job.
		IntAndStatus clusterIdAndStatus = schedd.newCluster(transaction);
		int clusterId = clusterIdAndStatus.getInteger();
		
		// Get a new Job ID (aka a ProcId) for the Job.
		IntAndStatus jobIdAndStatus = schedd.newJob(transaction, clusterId);
		int jobId = jobIdAndStatus.getInteger();
		
		// Build the Job's ClassAd.
		ClassAdStructAttr[] jobAd = buildJobAd(owner,
		        jobFileLocation,
		        clusterId,
		        jobId);
		
		// Submit the Job's ClassAd.
		schedd.submit(transaction, clusterId, jobId, jobAd);
		
		// Commit the transaction.
		schedd.commitTransaction(transaction);
		
		// Ask the Schedd to kick off the Job immediately.
		schedd.requestReschedule();
	}

} // class CondorUtils



/*
public class CondorUtils {
    @SuppressWarnings("unchecked")
    public static String submitToCondor(String condorUrl, 
            String jobName, String args, String scriptName, AuthToken auth,
            String clientGroups) throws JsonGenerationException, 
            JsonMappingException, IOException {
*/    	
    	// TODO: Submit job to Condor
    	
    	
    	
    	/*
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
        */
        // Map<String, Object> respObj = parseAweResponse(httpClient.execute(httpPost));
        // Map<String, Object> respData = (Map<String, Object>)respObj.get("data");
        // if (respData == null) throw new IllegalStateException("AWE error: " + respObj.get("error"));
        // String aweJobId = (String)respData.get("id");        
/*
        return jobName;    	
    }
    
}
*/