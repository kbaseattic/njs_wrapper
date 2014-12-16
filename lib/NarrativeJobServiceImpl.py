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

    def run_app(self, app):
        # self.ctx is set by the wsgi application class
        # return variables are: returnVal
        #BEGIN run_app
        #END run_app

        #At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method run_app return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]

    def check_app_state(self, job_id):
        # self.ctx is set by the wsgi application class
        # return variables are: returnVal
        #BEGIN check_app_state
        #END check_app_state

        #At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method check_app_state return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]

    def run_step(self, step):
        # self.ctx is set by the wsgi application class
        # return variables are: ujs_job_id
        #BEGIN run_step
        #END run_step

        #At some point might do deeper type checking...
        if not isinstance(ujs_job_id, basestring):
            raise ValueError('Method run_step return value ' +
                             'ujs_job_id is not type basestring as required.')
        # return the results
        return [ujs_job_id]

    def suspend_app(self, job_id):
        # self.ctx is set by the wsgi application class
        # return variables are: status
        #BEGIN suspend_app
        #END suspend_app

        #At some point might do deeper type checking...
        if not isinstance(status, basestring):
            raise ValueError('Method suspend_app return value ' +
                             'status is not type basestring as required.')
        # return the results
        return [status]

    def resume_app(self, job_id):
        # self.ctx is set by the wsgi application class
        # return variables are: status
        #BEGIN resume_app
        #END resume_app

        #At some point might do deeper type checking...
        if not isinstance(status, basestring):
            raise ValueError('Method resume_app return value ' +
                             'status is not type basestring as required.')
        # return the results
        return [status]

    def delete_app(self, job_id):
        # self.ctx is set by the wsgi application class
        # return variables are: status
        #BEGIN delete_app
        #END delete_app

        #At some point might do deeper type checking...
        if not isinstance(status, basestring):
            raise ValueError('Method delete_app return value ' +
                             'status is not type basestring as required.')
        # return the results
        return [status]

    def list_config(self):
        # self.ctx is set by the wsgi application class
        # return variables are: returnVal
        #BEGIN list_config
        #END list_config

        #At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method list_config return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]

    def ver(self):
        # self.ctx is set by the wsgi application class
        # return variables are: returnVal
        #BEGIN ver
        #END ver

        #At some point might do deeper type checking...
        if not isinstance(returnVal, basestring):
            raise ValueError('Method ver return value ' +
                             'returnVal is not type basestring as required.')
        # return the results
        return [returnVal]

    def status(self):
        # self.ctx is set by the wsgi application class
        # return variables are: returnVal
        #BEGIN status
        #END status

        #At some point might do deeper type checking...
        if not isinstance(returnVal, dict):
            raise ValueError('Method status return value ' +
                             'returnVal is not type dict as required.')
        # return the results
        return [returnVal]
