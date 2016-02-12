module NarrativeJobService {

    /* @range [0,1] */
    typedef int boolean;

    /* 
        A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is either the
        character Z (representing the UTC timezone) or the difference
        in time to UTC in the format +/-HHMM, eg:
            2012-12-17T23:24:06-0500 (EST time)
            2013-04-03T08:56:32+0000 (UTC time)
            2013-04-03T08:56:32Z (UTC time)
    */
    typedef string timestamp;

    /* A job id. */
    typedef string job_id;

    /*
        service_url could be empty in case of docker image of service loaded from registry,
        service_version - optional parameter defining version of service docker image.
    */
    typedef structure {
        string service_name;
        string method_name;
        string service_url;
        string service_version;
    } service_method;

    typedef structure {
        string service_name;
        string method_name;
        boolean has_files;
    } script_method;

   /*
        label - label of parameter, can be empty string for positional parameters
        value - value of parameter
        step_source - step_id that parameter derives from
        is_workspace_id - parameter is a workspace id (value is object name)
        # the below are only used if is_workspace_id is true
            is_input - parameter is an input (true) or output (false)
            workspace_name - name of workspace
            object_type - name of object type
    */
    typedef structure {
        string workspace_name;
        string object_type;
        boolean is_input;
    } workspace_object;

    typedef structure {
        string label;
        string value;
        string step_source;
        boolean is_workspace_id;
        workspace_object ws_object;
    } step_parameter;

    /*
        type - 'service' or 'script'.
        job_id_output_field - this field is used only in case this step is long running job and
            output of service method is structure with field having name coded in
            'job_id_output_field' rather than just output string with job id.
        method_spec_id - high level id of UI method used for logging of execution time 
            statistics.
    */
    typedef structure {
        string step_id;
        string type;
        service_method service;
        script_method script;
        list<step_parameter> parameters;
        list<UnspecifiedObject> input_values;
        boolean is_long_running;
        string job_id_output_field;
        string method_spec_id;
    } step;

    typedef structure {
        string name;
        list<step> steps;
    } app;

    typedef structure {
        int creation_time;
        int exec_start_time;
        int finish_time;
    } step_stats;

    /*
        job_id - id of job running app
        job_state - 'queued', 'running', 'completed', or 'error'
        running_step_id - id of step currently running
        step_outputs - mapping step_id to stdout text produced by step, only for completed or errored steps
        step_outputs - mapping step_id to stderr text produced by step, only for completed or errored steps
        step_job_ids - mapping from step_id to job_id
        step_stats - mapping from step_id to execution time statistics
    */
    typedef structure {
        job_id job_id;
        string job_state;
        string running_step_id;
        mapping<string, string> step_outputs;
        mapping<string, string> step_errors;
        mapping<string, string> step_job_ids;
        mapping<string, step_stats> step_stats;
        boolean is_deleted;
        app original_app;
    } app_state;

    typedef structure {
        boolean reboot_mode;
        boolean stopping_mode;
        int running_tasks_total;
        mapping<string, int> running_tasks_per_user;
        int tasks_in_queue;
        mapping<string, string> config;
        string git_commit;
    } Status;

    funcdef run_app(app app) returns (app_state) authentication required;

    funcdef check_app_state(job_id job_id) returns (app_state) authentication required;

    /*
        status - 'success' or 'failure' of action
    */
    funcdef suspend_app(job_id job_id) returns (string status) authentication required;

    funcdef resume_app(job_id job_id) returns (string status) authentication required;

    funcdef delete_app(job_id job_id) returns (string status) authentication required;

    funcdef list_config() returns (mapping<string, string>) authentication optional;

    /* Returns the current running version of the NarrativeJobService. */
    funcdef ver() returns (string);

    /* Simply check the status of this service to see queue details */
    funcdef status() returns (Status);

    funcdef list_running_apps() returns (list<app_state>) authentication optional;


    /*================================================================================*/
    /*  Running long running methods through Docker images of services from Registry  */
    /*================================================================================*/

    /*
        time - the time the call was started;
        method - service defined in standard JSON RPC way, typically it's
            module name from spec-file followed by '.' and name of funcdef
            from spec-file corresponding to running method (e.g.
            'KBaseTrees.construct_species_tree' from trees service);
        job_id - job id if method is asynchronous (optional field).
    */
    typedef structure {
        timestamp time;
        string method;
        job_id job_id;
    } MethodCall;

    /*
        call_stack - upstream calls details including nested service calls and 
            parent jobs where calls are listed in order from outer to inner.
    */
    typedef structure {
        list<MethodCall> call_stack;
        string run_id;
    } RpcContext;

    /*
        method - service defined in standard JSON RPC way, typically it's
            module name from spec-file followed by '.' and name of funcdef 
            from spec-file corresponding to running method (e.g.
            'KBaseTrees.construct_species_tree' from trees service);
        params - the parameters of the method that performed this call;
        service_ver - specific version of deployed service, last version is used 
            if this parameter is not defined (optional field);
        rpc_context - context of current method call including nested call history
            (optional field, could be omitted in case there is no call history);
        remote_url - optional field determining remote service call instead of
            local command line execution.
    */
    typedef structure {
        string method;
        list<UnspecifiedObject> params;
        string service_ver;
        RpcContext rpc_context;
        string remote_url;
    } RunJobParams;

    /* 
        Start a new job (long running method of service registered in ServiceRegistery).
        Such job runs Docker image for this service in script mode.
    */
    funcdef run_job(RunJobParams params) returns (job_id job_id) authentication required;

    /*
        Get job params necessary for job execution
    */
    funcdef get_job_params(job_id job_id) returns (RunJobParams params, 
        mapping<string, string> config) authentication required;

    typedef structure {
        string line;
        boolean is_error;
    } LogLine;

    funcdef add_job_logs(job_id job_id, list<LogLine> lines) 
        returns (int line_number) authentication required;

    /*
        skip_lines - optional parameter, number of lines to skip (in case they were 
            already loaded before).
    */
    typedef structure {
        job_id job_id;
        int skip_lines;
    } GetJobLogsParams;

    /*
        last_line_number - common number of lines (including those in skip_lines 
            parameter), this number can be used as next skip_lines value to
            skip already loaded lines next time.
    */
    typedef structure {
        list<LogLine> lines;
        int last_line_number;
    } GetJobLogsResults;

    funcdef get_job_logs(GetJobLogsParams params) returns (GetJobLogsResults)
        authentication required;

    /* Error block of JSON RPC response */
    typedef structure {
        string name;
        int code;
        string message;
        string error;
    } JsonRpcError;

    /*
        Either 'result' or 'error' field should be defined;
        result - keeps exact copy of what original server method puts
            in result block of JSON RPC response;
        error - keeps exact copy of what original server method puts
            in error block of JSON RPC response.
    */
    typedef structure {
        UnspecifiedObject result;
        JsonRpcError error;
    } FinishJobParams;

    /*
        Register results of already started job
    */
    funcdef finish_job(job_id job_id, FinishJobParams params) returns () authentication required;

    /*
        job_id - id of job running method
        finished - indicates whether job is done (including error cases) or not,
            if the value is true then either of 'returned_data' or 'detailed_error'
            should be defined;
        ujs_url - url of UserAndJobState service used by job service
        status - tuple returned by UserAndJobState.get_job_status method
        result - keeps exact copy of what original server method puts
            in result block of JSON RPC response;
        error - keeps exact copy of what original server method puts
            in error block of JSON RPC response.
    */
    typedef structure {
        string job_id;
        boolean finished;
        string ujs_url;
        UnspecifiedObject status;
        UnspecifiedObject result;
        JsonRpcError error;
    } JobState;

    /*
        Check if a job is finished and get results/error
    */
    funcdef check_job(job_id job_id) returns (JobState job_state) authentication required;
};
