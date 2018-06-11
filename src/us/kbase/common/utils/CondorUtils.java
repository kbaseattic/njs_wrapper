package us.kbase.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.util.Hash;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import us.kbase.auth.AuthToken;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class CondorUtils {


    public static final List<String> special_cases = Arrays.asList("request_cpus", "request_disk", "request_memory");

    /**
     * Create a condor submit file for Submitted Jobs
     * @param ujsJobId The UJS job id
     * @param token The token of the user of the submitted job
     * @param adminToken The admin token used for bind mounts, stored in configs
     * @param clientGroupsAndRequirements The AWE Client Group and an optional requirements statement
     * @param kbaseEndpoint The URL of the NJS Server
     * @param baseDir The Directory for the job to run in /mnt/awe/condor/username/JOBID
     * @return The generated condor submit file
     * @throws IOException
     */
    private static File createCondorSubmitFile(String ujsJobId, AuthToken token, AuthToken adminToken, String clientGroupsAndRequirements, String kbaseEndpoint, String baseDir, HashMap<String, String> optClassAds) throws IOException {

        HashMap<String, String> reqs = clientGroupsAndRequirements(clientGroupsAndRequirements);
        String clientGroups = reqs.get("client_group");
        String request_cpus = String.format("%s = %s",
                "request_cpus", reqs.getOrDefault("request_cpus", "1"));
        String request_memory = String.format("%s = %s",
                "request_memory", reqs.getOrDefault("request_memory", "5MB"));
        String request_disk = String.format("%s = %s",
                "request_disk", reqs.getOrDefault("request_disk", "1MB"));


        kbaseEndpoint = cleanCondorInputs(kbaseEndpoint);
        String executable = "/kb/deployment/misc/sdklocalmethodrunner.sh";
        String[] args = {ujsJobId, kbaseEndpoint};
        String arguments = String.join(" ", args);
        List<String> csf = new ArrayList<String>();
        csf.add("universe = vanilla");
        csf.add(String.format("accounting_group = %s", token.getUserName()));
        csf.add("+Owner = \"condor_pool\"");
        csf.add("universe = vanilla");
        csf.add("executable = " + executable);
        csf.add("ShouldTransferFiles = YES");
        csf.add("when_to_transfer_output = ON_EXIT");
        csf.add(request_cpus);
        csf.add(request_memory);
        csf.add(request_disk);
        csf.add("log    = logfile.txt");
        csf.add("output = outfile.txt");
        csf.add("error  = errors.txt");
        csf.add("getenv = true");
        csf.add("requirements = " + reqs.get("requirements_statement"));
        csf.add(String.format("environment = \"KB_AUTH_TOKEN=%s KB_ADMIN_AUTH_TOKEN=%s AWE_CLIENTGROUP=%s BASE_DIR=%s\"", token.getToken(), adminToken.getToken(), clientGroups, baseDir));
        csf.add("arguments = " + arguments);
        csf.add("batch_name = " + ujsJobId);
        if(optClassAds != null) {
            for (Map.Entry<String, String> pair : optClassAds.entrySet()) {
                csf.add(String.format("+%s = \"%s\"", pair.getKey(), pair.getValue()));
            }
        }
        csf.add("queue 1");

        System.out.println("ABOUT TO PRINT OUT" + String.format("%s.sub", ujsJobId));
        File submitFile = new File(String.format("%s.sub", ujsJobId));
        FileUtils.writeLines(submitFile, "UTF-8", csf);
        submitFile.setExecutable(true);
        return submitFile;
    }

    /**
     * Run the condor command and return
     * @param condorCommand command to run
     * @returnCondorResponse with STDIN and STDOUT
     * @throws IOException
     */
    public static CondorResponse runProcess(String[] condorCommand) throws IOException {
        //TODO DELETE PRINT STATEMENT
        System.out.println("Running command: [" + String.join(" ", condorCommand) + "]");
        final Process process = Runtime.getRuntime().exec(condorCommand);
        try {
            process.waitFor();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<String> stdOutMessage = IOUtils.readLines(process.getInputStream(), "UTF-8");
        List<String> stdErrMessage = IOUtils.readLines(process.getErrorStream(), "UTF-8");

        if (process.exitValue() != 0) {
            System.err.println("STDOUT:");
            for (String s : stdOutMessage) {
                System.err.println(s);
            }
            System.out.println("STDERR:");
            for (String s : stdOutMessage) {
                System.err.println(stdErrMessage);
            }
            return null;
            //throw new IOException("Error running condorCommand:" + String.join(" ", condorCommand));
        }
        return new CondorResponse(stdOutMessage, stdErrMessage);
    }


    /**
     * Parse out requirements form client groups
     * @param clientGroupsAndRequirements
     * @return
     */
    public static HashMap<String,String> clientGroupsAndRequirements(String clientGroupsAndRequirements) {

        String[] items = clientGroupsAndRequirements.split(",");
        String clientGroup = cleanCondorInputs(items[0]);
        HashMap<String,String> reqs = new HashMap<String ,String >();

        reqs.put("client_group", clientGroup);

        List<String> requirementsStatement = new ArrayList<String>();
        requirementsStatement.add(String.format("(CLIENTGROUP == \"%s\")", clientGroup));


        for(int i = 1; i < items.length; i++){
            String condorInput = cleanCondorInputs(items[i]);
            if(condorInput.contains("=")){
                String[] keyValue = condorInput.split("=");
                if(special_cases.contains(keyValue[0])){
                    reqs.put(keyValue[0],keyValue[1]);
                }
                else{
                    requirementsStatement.add(String.format("(%s == \"%s\")", keyValue[0],keyValue[1]));
                }
            }
            else{
                requirementsStatement.add(String.format("(%s)", condorInput));
            }
        }
        reqs.put("requirements_statement", "(" + String.join(" && ", requirementsStatement) + ")");
        return reqs;
    }


    /**
     * Remove spaces and maybe perform other logic
     * @param clientGroups to run the job with
     * @return String modified client groups
     */
    public static String clientGroupsToRequirements(String clientGroups) {
        List<String> requirementsStatement = new ArrayList<String>();
        for (String cg : clientGroups.split(",")) {
            cg = cleanCondorInputs(cg);
            requirementsStatement.add(String.format("(CLIENTGROUP == \"%s\")", cg));
        }
        return "(" + String.join(" || ", requirementsStatement) + ")";
    }

    /**
     * Remove space, remove single quote, and remove double quotes.
     * @param input
     * @return cleaned string
     */
    public static String cleanCondorInputs(String input) {
        return input.replaceAll("[^0-9A-Za-z=_]","");
    }

    /**
     * Call condor_submit with the ujsJobId as batch job name
     * @param ujsJobId The UJS job id
     * @param token The token of the user of the submitted job
     * @param clientGroups The AWE Client Group
     * @param kbaseEndpoint The URL of the NJS Server
     * @param baseDir The Directory for the job to run in /mnt/awe/condor/username/JOBID
     * @param adminToken The admin token used for bind mounts, stored in configs
     * @return String condor job id Range
     * @throws Exception
     */
    public static String submitToCondorCLI(String ujsJobId, AuthToken token, String clientGroups, String kbaseEndpoint, String baseDir, HashMap<String,String> optClassAds, AuthToken adminToken) throws Exception {
        File condorSubmitFile = createCondorSubmitFile(ujsJobId, token, adminToken, clientGroups, kbaseEndpoint, baseDir, optClassAds);
        String[] cmdScript = {"condor_submit", "-spool", "-terse", condorSubmitFile.getAbsolutePath()};
        String jobID = null;
        int retries = 10;

        while (jobID == null && retries > 0) {
            jobID = runProcess(cmdScript).stdout.get(0);
            retries--;
        }
        if (jobID == null) {
            throw new IOException("Error running condorCommand:" + String.join(" ", cmdScript));
        }
        condorSubmitFile.delete();
        return jobID;
    }

    /**
     * Call condor_q with the ujsJobId a string target to filter condor_q
     * @param ujsJobId to get job JobPrio for
     * @param attribute attribute to search condorQ for
     * @return String condor job attribute or NULL
     */
    public static String condorQ(String ujsJobId, String attribute) throws IOException, InterruptedException {
        int retries = 3;
        String result = null;
        String[] cmdScript = new String[]{"/kb/deployment/misc/condor_q.sh", ujsJobId, attribute};
        while (result == null && retries > 0) {
            Thread.sleep(5000);
            result = String.join("\n", runProcess(cmdScript).stdout);
            // convert JSON string to Map There has to be a better way than this
            if (result.contains(attribute)) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> myObjects =
                            mapper.readValue(result, new TypeReference<List<Map<String, Object>>>() {
                            });
                    result = myObjects.get(0).get(attribute).toString();
                } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
                    result = null;
                }
            } else {
                result = null;
            }
            retries--;
        }
        return result;
    }

    /**
     * Get a list of jobs ids and job statuses for all jobs recorded in condor
     * @return A List of job IDS and their respective statuses.
     * @throws Exception
     */
    public static HashMap<String, String> getAllJobStates() throws Exception {
        String[] cmdScript = new String[]{"condor_q", "-af", "JobBatchName", "LastJobStatus"};
        List<String> processResult = runProcess(cmdScript).stdout;
        HashMap<String, String> JobStates = new HashMap<>();
        for (String line : processResult) {
            String[] idStatusLine = line.split(" ");
            JobStates.put(idStatusLine[0], idStatusLine[1]);
        }
        return JobStates;
    }
    /**
     * Get job state from condor_q with the LastJobStatus param
     * @param ujsJobId ujsJobId to get job state for
     * @return String  condor job state or NULL
     */
    public static String getJobState(String ujsJobId) throws Exception {
        return condorQ(ujsJobId, "LastJobStatus");
    }
    /**
     * Get job priority from condor_q with the JobPrio param
     * @param ujsJobId ujsJobId to get job JobPrio for
     * @return String  condor job priority or NULL
     */
    public static String getJobPriority(String ujsJobId) throws Exception {
        return condorQ(ujsJobId, "JobPrio");
    }
    /**
     * Remove condor jobs with a given batch name
     * @param ujsJobID ujsJobId for the job batch name
     * @return Result of the condor_rm command
     */
    public static String condorRemoveJobRange(String ujsJobID) throws Exception {

        String[] cmdScript = new String[]{"/kb/deployment/misc/condor_rm.sh", ujsJobID};
        String processResult = runProcess(cmdScript).stdout.get(0);
        return processResult;

        //TODO : CALL NJS and CANCEL THE JOB

    }
} // class CondorUtils
