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

import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
	
	
public class CondorUtils
{

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
 
	
	
	public static float submitToCondorCLI ( String ujsJobId, String selfExternalUrl, String submitFilePath, String aweClientGroups ) throws IOException
	{
		float jobId = 0;
		String line = "";
		int exitVal = 0;

		Runtime r = Runtime.getRuntime();

		// TODO: Change path to condor_submit script to a relative path: like ../scripts/condor_submit.sh
		String[] cmdScript = new String[]{ "/bin/bash", "/home/submitter/submit/njs_wrapper/scripts/condor_submit.sh",
				ujsJobId,
				submitFilePath };
		
		// Execute job submit script with submitFilePath as the submit fle path:
		Process p = r.exec( cmdScript );
		
		try {
			p.waitFor();
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
		/*
		BufferedReader b = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		
		line = b.readLine();
		System.out.println(line);		
		while (( line = b.readLine()) != null ) {
		  System.out.println(line);
		  if( line.contains( "** Proc" ) ) break;
		}
		b.close();
		
		if( line == null || !line.contains( "** Proc" ) ) {
			System.err.println( "ERROR ERROR ERROR: CondorUtils::submitToCondorCLI: Could not parse jobId from condor_submit IO" );			
            throw new IOException(  "CondorUtils::submitToCondorCLI: Could not parse jobId from condor_submit IO" ); 			
		}
		*/
		exitVal = p.exitValue();		
		if ( exitVal == 0 ) { // success
			
			// XXX: Determine job id (cluster id dot zero) of the job just submitted???			
	        // XXX: parse the command return string???
			// TODO: Don't parse it... FORCE it!!! Force a 'batch' job id (coordianted with ujs/catalog)
			//     Go back... in the call stack heirarchy (added param to condor_submit.sh to force a 'batch' job id)
			/*
			try{
			    jobId = Integer.valueOf( line.substring( (line.indexOf("** Proc") + 7), line.length() ) );
			    
		        System.out.println( "CondorUtils::submitToCondorCLI::jobId = " + jobId );			
			} catch (NumberFormatException e) {
				System.err.println( "ERROR ERROR ERROR: CondorUtils::submitToCondorCLI: Could not parse jobId from condor_submit IO" );			
	            throw new IOException(  "CondorUtils::submitToCondorCLI: Could not parse jobId from condor_submit IO" ); 
			}			    
			*/
		} else {
			System.err.println( "ERROR ERROR ERROR: CondorUtils::submitToCondorCLI: EXIT value from process calling condor_submit came back non-zero; for command:\n"
					            + cmdScript.toString() );			
            throw new IOException(  "CondorUtils::submitToCondorCLI: EXIT value from process calling condor_submit came back non-zero" ); 
		}

		// TODO: Refactor methods's return type (force 'batch' job id ===>> do we still want to return it?
		return jobId;
	}

	
    // Test main for submitting via CLI system calls to Condor
	// Usage: submitToCondorCLI <contents to go in the submit file>
	public static void main(String[] arguments)
	{
		String ujsJobId = "";
		String selfExternalUrl = "";
		String submitFilePath = "";
		String aweClientGroups = "SCHEDD";

		
        if( ! ( arguments.length > 0 ) ) {
        	// Debug: defaults:
        	ujsJobId = "condor@condor";
        	
        	// Debug: just do a 'uname -a'
        	submitFilePath = "job_exec01.sub";
        	
        } else if( arguments.length == 1 ){
        	ujsJobId = arguments[ 0 ];
        	
        	// Debug: just do a 'uname -a'
        	submitFilePath = "job_exec01.sub";
        	
        } else if( arguments.length == 2 ){
        	
        	ujsJobId = arguments[ 0 ];
        	
        	selfExternalUrl = arguments[ 1 ];
        	
        	// Debug: just do a 'uname -a'
        	submitFilePath = "job_exec01.sub";
        } else if( arguments.length == 3 ){
        	
        	ujsJobId = arguments[ 0 ];
        	
        	selfExternalUrl = arguments[ 1 ];
        	
        	submitFilePath = arguments[ 2 ];
        }
	    // Call submitToCondorCLI with submitFilePath
	    try {
	    	float jobId =  submitToCondorCLI( ujsJobId, selfExternalUrl, submitFilePath, aweClientGroups );
	    	
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
	
	
} // class CondorUtils
