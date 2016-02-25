#BEGIN_HEADER
#END_HEADER


class NarrativeJobService:
    '''
    Module Name:
    NarrativeJobService

    Module Description:
    
    '''

    ######## WARNING FOR GEVENT USERS #######
    # Since asynchronous IO can lead to methods - even the same method -
    # interrupting each other, you must be *very* careful when using global
    # state. A method could easily clobber the state set by another while
    # the latter method is running.
    #########################################
    #BEGIN_CLASS_HEADER
    #END_CLASS_HEADER

    # config contains contents of config file in a hash or None if it couldn't
    # be found
    def __init__(self, config):
        #BEGIN_CONSTRUCTOR
        #END_CONSTRUCTOR
        pass

    def run_app(self, ctx, app):
        # ctx is the context object
        # return variables are: returnVal
        #BEGIN run_app
        #END run_app

        # At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method run_app return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]

    def check_app_state(self, ctx, job_id):
        # ctx is the context object
        # return variables are: returnVal
        #BEGIN check_app_state
        #END check_app_state

        # At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method check_app_state return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]

    def suspend_app(self, ctx, job_id):
        # ctx is the context object
        # return variables are: status
        #BEGIN suspend_app
        #END suspend_app

        # At some point might do deeper type checking...
        if not isinstance(status, basestring):
            raise ValueError('Method suspend_app return value ' +
                             'status is not type basestring as required.')
        # return the results
        return [status]

    def resume_app(self, ctx, job_id):
        # ctx is the context object
        # return variables are: status
        #BEGIN resume_app
        #END resume_app

        # At some point might do deeper type checking...
        if not isinstance(status, basestring):
            raise ValueError('Method resume_app return value ' +
                             'status is not type basestring as required.')
        # return the results
        return [status]

    def delete_app(self, ctx, job_id):
        # ctx is the context object
        # return variables are: status
        #BEGIN delete_app
        #END delete_app

        # At some point might do deeper type checking...
        if not isinstance(status, basestring):
            raise ValueError('Method delete_app return value ' +
                             'status is not type basestring as required.')
        # return the results
        return [status]

    def list_config(self, ctx):
        # ctx is the context object
        # return variables are: returnVal
        #BEGIN list_config
        #END list_config

        # At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method list_config return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]

    def ver(self, ctx):
        # ctx is the context object
        # return variables are: returnVal
        #BEGIN ver
        #END ver

        # At some point might do deeper type checking...
        if not isinstance(returnVal, basestring):
            raise ValueError('Method ver return value ' +
                             'returnVal is not type basestring as required.')
        # return the results
        return [returnVal]

    def status(self, ctx):
        # ctx is the context object
        # return variables are: returnVal
        #BEGIN status
        #END status

        # At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method status return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]

    def list_running_apps(self, ctx):
        # ctx is the context object
        # return variables are: returnVal
        #BEGIN list_running_apps
        #END list_running_apps

        # At some point might do deeper type checking...
        if not isinstance(returnVal, list):
            raise ValueError('Method list_running_apps return value ' +
                             'returnVal is not type list as required.')
        # return the results
        return [returnVal]

    def run_job(self, ctx, params):
        # ctx is the context object
        # return variables are: job_id
        #BEGIN run_job
        #END run_job

        # At some point might do deeper type checking...
        if not isinstance(job_id, basestring):
            raise ValueError('Method run_job return value ' +
                             'job_id is not type basestring as required.')
        # return the results
        return [job_id]

    def get_job_params(self, ctx, job_id):
        # ctx is the context object
        # return variables are: params, config
        #BEGIN get_job_params
        #END get_job_params

        # At some point might do deeper type checking...
        if not isinstance(params, dict):
            raise ValueError('Method get_job_params return value ' +
                             'params is not type dict as required.')
        if not isinstance(config, dict):
            raise ValueError('Method get_job_params return value ' +
                             'config is not type dict as required.')
        # return the results
        return [params, config]

    def add_job_logs(self, ctx, job_id, lines):
        # ctx is the context object
        # return variables are: line_number
        #BEGIN add_job_logs
        #END add_job_logs

        # At some point might do deeper type checking...
        if not isinstance(line_number, int):
            raise ValueError('Method add_job_logs return value ' +
                             'line_number is not type int as required.')
        # return the results
        return [line_number]

    def get_job_logs(self, ctx, params):
        # ctx is the context object
        # return variables are: returnVal
        #BEGIN get_job_logs
        #END get_job_logs

        # At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method get_job_logs return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]

    def finish_job(self, ctx, job_id, params):
        # ctx is the context object
        #BEGIN finish_job
        #END finish_job

    def check_job(self, ctx, job_id):
        # ctx is the context object
        # return variables are: job_state
        #BEGIN check_job
        #END check_job

        # At some point might do deeper type checking...
        if not isinstance(job_state, dict):
            raise ValueError('Method check_job return value ' +
                             'job_state is not type dict as required.')
        # return the results
        return [job_state]
