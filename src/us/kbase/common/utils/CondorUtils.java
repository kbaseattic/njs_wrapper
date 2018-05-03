package us.kbase.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import us.kbase.auth.AuthToken;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CondorUtils {

    private static File createCondorSubmitFile(String ujsJobId, AuthToken token, AuthToken adminToken, String clientGroups, String kbaseEndpoint, String baseDir) throws IOException {


        String clientGroupsNew = clientGroupsToRequirements(clientGroups);
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
        csf.add("request_cpus = 1");
        csf.add("request_memory = 5MB");
        csf.add("request_disk = 1MB");
        csf.add("log    = logfile.txt");
        csf.add("output = outfile.txt");
        csf.add("error  = errors.txt");
        csf.add("getenv = true");
        csf.add("requirements = " + clientGroupsNew);
        csf.add(String.format("environment = \"KB_AUTH_TOKEN=%s KB_ADMIN_AUTH_TOKEN=%s AWE_CLIENTGROUP=%s BASE_DIR=%s\"", token.getToken(), adminToken.getToken(), clientGroups, baseDir));
        csf.add("arguments = " + arguments);
        csf.add("batch_name = " + ujsJobId);
        csf.add("queue 1");

        File submitFile = new File(String.format("%s.sub", ujsJobId));
        FileUtils.writeLines(submitFile, "UTF-8", csf);
        submitFile.setExecutable(true);

        return submitFile;
    }

    public static CondorResponse runProcess(String[] condorCommand) throws IOException {
        /**
         * Run the condor command and return
         * @param condorCommand command to run
         * @return CondorResponse with STDIN and STDOUT
         */
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

    public static String clientGroupsToRequirements(String clientGroups) {
        /**
         * Remove spaces and maybe perform other logic
         * @param clientGroups to run the job with
         * @return String modified client groups
         */
        //TODO REMOVE THIS OR UPDATE METHODS
        if (clientGroups.equals("ci")) {
            clientGroups = "njs";
        }
        List<String> requirementsStatement = new ArrayList<String>();
        for (String cg : clientGroups.split(",")) {
            cg = cleanCondorInputs(cg);
            requirementsStatement.add(String.format("(CLIENTGROUP == \"%s\")", cg));
        }
        return "(" + String.join(" || ", requirementsStatement) + ")";
    }

    public static String cleanCondorInputs(String input) {
        return input.replace(" ", "")
                .replace("'", "")
                .replace("\"", "");
    }

    public static String submitToCondorCLI(String ujsJobId, AuthToken token, String clientGroups, String kbaseEndpoint, String baseDir, AuthToken adminToken) throws Exception {
        /**
         * Call condor_submit with the ujsJobId as batch job name
         * @param jobID ujsJobId to name the batch job with
         * @param token token to place into the shell script
         * @return String condor job id
         */

        File condorSubmitFile = createCondorSubmitFile(ujsJobId, token, adminToken , clientGroups, kbaseEndpoint, baseDir);
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


    public static String condorQ(String ujsJobId, String attribute) throws IOException, InterruptedException {
        /**
         * Call condor_q with the ujsJobId a string target to filter condor_q
         * @param jobID ujsJobId to get job JobPrio for
         * @param attribute attribute to search condorQ for
         * @return String condor job attribute or NULL
         */
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

    public static String getJobState(String ujsJobId) throws Exception {
        /**
         * Get job state from condor_q with the LastJobStatus param
         * @param jobID ujsJobId to get job state for
         * @return String  condor job state or NULL
         */
        return condorQ(ujsJobId, "LastJobStatus");
    }

    public static String getJobPriority(String ujsJobId) throws Exception {
        /**
         * Get job priority from condor_q with the JobPrio param
         * @param jobID ujsJobId to get job JobPrio for
         * @return String  condor job priority or NULL
         */
        return condorQ(ujsJobId, "JobPrio");
    }

    public static String condorRemoveJobRange(String ujsJobID) throws Exception {
        /**
         * Remove condor jobs with a given batch name
         * @param jobID ujsJobId for the job batch name
         * @return Result of the condor_rm command
         */
        String[] cmdScript = new String[]{"/kb/deployment/misc/condor_rm.sh", ujsJobID};
        String processResult = runProcess(cmdScript).stdout.get(0);
        return processResult;

        //TODO : CALL NJS and CANCEL THE JOB

    }
} // class CondorUtils
