#BEGIN_HEADER
#END_HEADER


class NJSMock:
    '''
    Module Name:
    NJSMock

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

    def check_app_state(self, app_run_id):
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
