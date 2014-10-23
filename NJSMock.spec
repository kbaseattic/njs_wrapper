module NJSMock {

    /* @range [0,1] */
    typedef int boolean;

    typedef structure {
        string service_url;
        string method_name;
    } generic_service_method;

    typedef structure {
        string python_class;
        string method_name;
    } python_backend_method;

    typedef structure {
        string script_name;
    } commandline_script_method;

    /*
        type - 'generic', 'python' or 'script'.
    */
    typedef structure {
        string step_id;
        string type;
        generic_service_method generic;
        python_backend_method python;
        commandline_script_method script;
        list<UnspecifiedObject> input_values;
        boolean is_long_running;
        string job_id_output_field;
    } step;

    typedef structure {
        string app_run_id;
        list<step> steps;
    } app;

    /*
        step_jobs - mapping from step_id to job_id.
    */
    typedef structure {
        string app_job_id;
        mapping<string, string> step_job_ids;
        mapping<string, UnspecifiedObject> step_outputs;
    } app_jobs;

    funcdef run_app(app app) returns (app_jobs) authentication required;

    funcdef check_app_state(string app_run_id) returns (app_jobs) authentication required;

};
