package us.kbase.common.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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


    // Dump condor_q for the job id
    public static Map<String, Object> getJobDescr( /* String condorUrl */ String jobId ) throws IOException {
    	
		// XXX: Hardcoded path to the script to execute:
		String[] cmdScript = new String[]{"/bin/bash", "/home/submitter/submit/njs_wrapper/scripts/condor_q.sh", jobId};
    	
    	Map<String, Object> respObj = null;
		String message = "";
		
		Process p = Runtime.getRuntime().exec(cmdScript);
		
        BufferedReader reader = new BufferedReader(new InputStreamReader( p.getInputStream() ));
        String line = reader.readLine();
        System.out.println( line );
        line = reader.readLine();
        message += line;
        while ( line != null ) {    
            System.out.println( line );
            line = reader.readLine();
            message += line;
        }
        return respObj;

    }

    
    
    // Get job status for job id
    // U = unexpanded (never been run), H = on hold, R = running, I = idle (waiting for a machine to execute on), C = completed, and X = removed
    public static Integer getJobState( String jobId ) throws IOException, ClientProtocolException, JsonParseException, JsonMappingException {

        Map<String, Object> respObj = new LinkedHashMap<String, Object> ();

        // Query the status int out of response:
        int status = 1;                
        
		// XXX: Hardcoded path to the script to execute:
		String[] cmdScript = new String[]{"/bin/bash", "/home/submitter/submit/njs_wrapper/scripts/condor_q_long.sh", jobId, "LastJobStatus"};
		
		Process p = Runtime.getRuntime().exec( cmdScript );
		
        BufferedReader reader = new BufferedReader(new InputStreamReader( p.getInputStream() ));
        String line = reader.readLine();
        System.out.println( line );

        // parse the substring after '=' from line
        // Gets NPE for job id of bogusJobId
        status = Integer.valueOf( line.substring( (line.indexOf("=") + 2), line.length() ) );
        respObj.put( "Status" , status);
        System.out.println( "CondorUtils::parseResponse::status = " + status );
        return status;
    }
        
    public static boolean checkJobIsNotDone(String jobId) {
    	boolean b = true;
    	Integer status = null;
    	try{
    		status = getJobState( jobId );
    		b = status < 5;
    		return b;
    	
	    } catch( IOException ex ) {
            ex.printStackTrace();

            String message = "CondorUtils: Error calling checkJobIsNotDone "  + ex.getMessage();
            System.err.println(message);
            return b;
        }
        // return aweState.equals("init") || aweState.equals("queued") || aweState.equals("in-progress");
    }
    
    public static boolean checkJobIsDoneWithoutError(String jobId) {
    	boolean b = true;
    	Integer status = null;
    	try{
    		status = getJobState( jobId );
    		b = status == 5;
    		return b;
    	
	    } catch( IOException ex ) {
            ex.printStackTrace();

            String message = "CondorUtils: Error calling checkJobIsDoneWithoutError "  + ex.getMessage();
            System.err.println(message);
            return b;
        }
    	// return aweState.equals("completed");
    }       
        
        
    // Parse job status for job id
    public static Map<String, Object> parseResponse( String jobId ) throws IOException {

        Map<String, Object> respObj = new LinkedHashMap<String, Object> ();

        // Query the status int out of response:
        int status = 1;                
        
		// XXX: Hardcoded path to the script to execute:
		String[] cmdScript = new String[]{"/bin/bash", "/home/submitter/submit/njs_wrapper/scripts/condor_q_long.sh", jobId, "LastJobStatus"};
		
		Process p = Runtime.getRuntime().exec( cmdScript );
		
        BufferedReader reader = new BufferedReader(new InputStreamReader( p.getInputStream() ));
        String line = reader.readLine();
        System.out.println( line );

        // parse the substring after '=' from line
        // Gets NPE for job id of bogusJobId
        status = Integer.valueOf( line.substring( (line.indexOf("=") + 2), line.length() ) );
        respObj.put( "Status" , status);
        System.out.println( "CondorUtils::parseResponse::status = " + status );
        
        return respObj;
    }
    

    
    public static Map<String, Object> getJobPosition( String jobId ) throws IOException {
        Map<String, Object> respObj = new LinkedHashMap<String, Object> ();

        // With Condor:  Is there a notion of Position?
        // Condor:: Not FIFO… Policy based with a lot of moving parts to it.
        // There is a notion of Rank but that’s just a policy attribute not a deterministic position in the execution queue or pool
        // Parse "PRIO" out of condor_q response

        int priority = 0;  // Range is -20 to 20, positive is high                
        
		// XXX: Hardcoded path to the script to execute:
		String[] cmdScript = new String[]{"/bin/bash", "/home/submitter/submit/njs_wrapper/scripts/condor_q_long.sh", jobId, "JobPrio"};
		
		Process p = Runtime.getRuntime().exec( cmdScript );
		
        BufferedReader reader = new BufferedReader(new InputStreamReader( p.getInputStream() ));
        String line = reader.readLine();
        System.out.println( line );

        // parse the substring after '=' from line
        // Gets NPE for job id of bogusJobId
        priority = Integer.valueOf( line.substring( (line.indexOf("=") + 2), line.length() ) );
        respObj.put( "Priority" , priority);
        System.out.println( "CondorUtils::parseResponse::priority = " + priority );
        

        
        return respObj;
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
	
	
	
	public static int submitToCondorCLI ( String submitFilePath ) throws IOException
	{
		int jobId = 0;
		String line = "";
		Runtime r = Runtime.getRuntime();
		
		// XXX: Hardcoded path to the script dir:
		// cd to the scripts directory and purge out temp.sub
		// Process p = r.exec( "cd /home/submitter/submit/njs_wrapper/scripts" );
		// r.exec( "rm -f temp.sub" );
		
		// Build a file in njs_wrapper/scripts directory named temp.sh
		// TODO: will later become the 'executable' attribute inside the submit file 
		// p = r.exec( "echo " + submitFilePath + " > temp.sh");

		String[] cmdScript = new String[]{ "/bin/bash", "/home/submitter/submit/njs_wrapper/scripts/condor_submit.sh",
				submitFilePath };
		
		// Execute job submit script with temp.sub as the submit fle:
		Process p = r.exec( cmdScript );
		
		BufferedReader b = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		while ((line = b.readLine()) != null) {
		  System.out.println(line);
		}
		b.close();		
		
		return jobId;
	}

	
    // Test main for submitting via CLI system calls to Condor
	// Usage: submitToCondorCLI <contents to go in the submit file>
	public static void main(String[] arguments)
	{
		String submitFilePath;
        if( ! ( arguments.length > 0 ) ) {
        	// Debug: just do a 'uname -a'
        	submitFilePath = "job_exec01.sub";
        } else {
        	submitFilePath = arguments[ 0 ];
        }
        
	    // Call submitToCondorCLI with submitFilePath
	    try {
	    	int jobId =  submitToCondorCLI( submitFilePath );
	    	
	    } catch( Exception ex ) {
            ex.printStackTrace();

            String message = "CondorUtils: Error calling submitToCondorCLI from main... "  + ex.getMessage();
            System.err.println(message);
        }
	}
	
	// Test main for exercising the query methods
	/*
	public static void main(String[] arguments)
	{
		String jobId;
        if( ! ( arguments.length > 0 ) ) {
        	jobId = "BogusJobId";
        	
        } else {
			jobId = arguments[ 0 ];
			// URL scheddLocation = new URL( arguments[ 0 ] );
        }
        
	    // Call parseResponse
        // Usage: CondorUtils $1
        try{
				Map<String, Object> respObj = parseResponse( jobId );
	    	    
		} catch( IOException ex ) {
	            ex.printStackTrace();
	
	            String message = "CondorUtils: Error calling parseResponse from main... "  + ex.getMessage();
	            System.err.println(message);
		}
			
	    // Call getJobDescr
        // Usage: CondorUtils $1
        try{
				Map<String, Object> respObj = getJobDescr( jobId );    
		} catch( IOException ex ) {
	            ex.printStackTrace();
	
	            String message = "CondorUtils: Error calling getJobDescr from main... "  + ex.getMessage();
	            System.err.println(message);
	            // if (log != null) log.logErr(message);
		}
	
	}
    */
    // Test main for submitting via Spinning API
    /*
	public static void main(String[] arguments)
	{
		URL scheddLocation = new URL(arguments[0]);
		String owner = arguments[1];
		String jobFileLocation = arguments[2];
		
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
		
		// Debug: Dump JobAd
		System.out.println( "CondorUtils::Dump JobAd: " + jobAd.toString() );
		for( int i = 0; i < jobAd.length; i++ ) {
			System.out.println( "    JobAd[ " + i + " ] name = " + jobAd[ i ].getName() + "    JobAd[ " + i + " ] value = " + jobAd[ i ].getValue() );
		}
					
		// Commit the transaction.
		schedd.commitTransaction(transaction);
		
		// Ask the Schedd to kick off the Job immediately.
		schedd.requestReschedule();
	}
	*/

} // class CondorUtils
