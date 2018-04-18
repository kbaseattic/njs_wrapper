package us.kbase.common.utils;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;


import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import us.kbase.auth.AuthToken;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;


import us.kbase.common.utils.CondorResponse;

public class CondorUtils {

    private static File createCondorSubmitFile(String ujsJobId, String token, String clientGroups, String kbaseEndpoint, String baseDir) throws IOException {

        clientGroups = clientGroupsToRequirements(clientGroups);
        kbaseEndpoint = cleanCondorInputs(kbaseEndpoint);
        String executable = "/kb/deployment/misc/sdklocalmethodrunner.sh";
        String[] args = {  ujsJobId, kbaseEndpoint};
        String arguments = String.join(" ",args);
        List<String> csf = new ArrayList<String>();
        csf.add("universe = vanilla");
        csf.add("accounting_group = sychan");
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
        csf.add("requirements = " + clientGroups);
        csf.add(String.format("environment = \"KB_DOCKER_NETWORK=minikb_default KB_AUTH_TOKEN=%s\"", token));
        csf.add("arguments = " + arguments);
        csf.add("batch_name = " + ujsJobId);
        csf.add("queue 1");

        File submitFile = new File(String.format("%s/%s/submitscript.sub", baseDir,ujsJobId ) );
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
            throw new IOException("Error running condorCommand:" + String.join(" ", condorCommand));
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
        if(clientGroups.equals("ci")){
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


    public static String submitToCondorCLI(String ujsJobId, String token, String clientGroups, String kbaseEndpoint, String baseDir) throws IOException {
        /**
         * Call condor_submit with the ujsJobId as batch job name
         * @param jobID ujsJobId to name the batch job with
         * @param token token to place into the shell script
         * @return String condor job id
         */

        String condorSubmitFile = createCondorSubmitFile(ujsJobId, token, clientGroups, kbaseEndpoint, baseDir).getAbsolutePath();
        String[] cmdScript = {"condor_submit", "-spool" ,"-terse" , condorSubmitFile};
        return runProcess(cmdScript).stdout.get(0);
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
        } else {
            return null;
        }
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


} // class CondorUtils
