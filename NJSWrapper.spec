module NarrativeJobService {

    /* @range [0,1] */
    typedef int boolean;

    typedef structure {
        string service_name;
        string method_name;
        string service_url;
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
    } step;

    typedef structure {
        string name;
        list<step> steps;
    } app;

    /*
        job_id - id of job running app
        job_state - 'queued', 'running', 'completed', or 'error'
        running_step_id - id of step currently running
        step_outputs - mapping step_id to stdout text produced by step, only for completed or errored steps
        step_outputs - mapping step_id to stderr text produced by step, only for completed or errored steps
        step_job_ids - mapping from step_id to job_id.
    */
    typedef structure {
        string job_id;
        string job_state;
        string running_step_id;
        /*mapping<string, string> step_job_ids;*/
        mapping<string, string> step_outputs;
        mapping<string, string> step_errors;
        boolean is_deleted;
    } app_state;

    typedef structure {
    	boolean reboot_mode;
    	boolean stopping_mode;
    	int running_tasks_total;
    	mapping<string, int> running_tasks_per_user;
    	int tasks_in_queue;
    } Status;

    funcdef run_app(app app) returns (app_state) authentication required;

    funcdef check_app_state(string job_id) returns (app_state) authentication required;

    /*funcdef run_step(step step) returns (string ujs_job_id) authentication required;*/

    /*
        status - 'success' or 'failure' of action
    */
    funcdef suspend_app(string job_id) returns (string status) authentication required;

    funcdef resume_app(string job_id) returns (string status) authentication required;

    funcdef delete_app(string job_id) returns (string status) authentication required;

    funcdef list_config() returns (mapping<string, string>) authentication optional;
    
    /* Returns the current running version of the NarrativeJobService. */
    funcdef ver() returns (string);
    
    /* Simply check the status of this service to see queue details */
    funcdef status() returns (Status);

    funcdef list_running_apps() returns (list<app_state>) authentication optional;
};
