

function NarrativeJobService(url, auth, auth_cb, timeout, async_job_check_time_ms, async_version) {
    var self = this;

    this.url = url;
    var _url = url;

    this.timeout = timeout;
    var _timeout = timeout;
    
    this.async_job_check_time_ms = async_job_check_time_ms;
    if (!this.async_job_check_time_ms)
        this.async_job_check_time_ms = 5000;
    this.async_version = async_version;

    if (typeof(_url) != "string" || _url.length == 0) {
        _url = "https://kbase.us/services/njs_wrapper/";
    }
    var _auth = auth ? auth : { 'token' : '', 'user_id' : ''};
    var _auth_cb = auth_cb;

    this._check_job = function (job_id, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 3)
            throw 'Too many arguments ('+arguments.length+' instead of 3)';
        return json_call_ajax("NarrativeJobService._check_job", 
            [job_id], 1, _callback, _errorCallback);
    };


     this.run_app = function (app, _callback, _errorCallback) {
        if (typeof app === 'function')
            throw 'Argument app can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.run_app",
            [app], 1, _callback, _errorCallback);
    };
 
     this.check_app_state = function (job_id, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.check_app_state",
            [job_id], 1, _callback, _errorCallback);
    };
 
     this.suspend_app = function (job_id, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.suspend_app",
            [job_id], 1, _callback, _errorCallback);
    };
 
     this.resume_app = function (job_id, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.resume_app",
            [job_id], 1, _callback, _errorCallback);
    };
 
     this.delete_app = function (job_id, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.delete_app",
            [job_id], 1, _callback, _errorCallback);
    };
 
     this.list_config = function (_callback, _errorCallback) {
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 0+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(0+2)+')';
        return json_call_ajax("NarrativeJobService.list_config",
            [], 1, _callback, _errorCallback);
    };
 
     this.ver = function (_callback, _errorCallback) {
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 0+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(0+2)+')';
        return json_call_ajax("NarrativeJobService.ver",
            [], 1, _callback, _errorCallback);
    };
 
     this.status = function (_callback, _errorCallback) {
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 0+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(0+2)+')';
        return json_call_ajax("NarrativeJobService.status",
            [], 1, _callback, _errorCallback);
    };
 
     this.list_running_apps = function (_callback, _errorCallback) {
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 0+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(0+2)+')';
        return json_call_ajax("NarrativeJobService.list_running_apps",
            [], 1, _callback, _errorCallback);
    };
 
     this.run_job = function (params, _callback, _errorCallback) {
        if (typeof params === 'function')
            throw 'Argument params can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.run_job",
            [params], 1, _callback, _errorCallback);
    };
 
     this.get_job_params = function (job_id, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.get_job_params",
            [job_id], 2, _callback, _errorCallback);
    };
 
     this.add_job_logs = function (job_id, lines, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (typeof lines === 'function')
            throw 'Argument lines can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 2+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(2+2)+')';
        return json_call_ajax("NarrativeJobService.add_job_logs",
            [job_id, lines], 1, _callback, _errorCallback);
    };
 
     this.get_job_logs = function (params, _callback, _errorCallback) {
        if (typeof params === 'function')
            throw 'Argument params can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.get_job_logs",
            [params], 1, _callback, _errorCallback);
    };
 
     this.finish_job = function (job_id, params, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (typeof params === 'function')
            throw 'Argument params can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 2+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(2+2)+')';
        return json_call_ajax("NarrativeJobService.finish_job",
            [job_id, params], 0, _callback, _errorCallback);
    };
 
     this.check_job = function (job_id, _callback, _errorCallback) {
        if (typeof job_id === 'function')
            throw 'Argument job_id can not be a function';
        if (_callback && typeof _callback !== 'function')
            throw 'Argument _callback must be a function if defined';
        if (_errorCallback && typeof _errorCallback !== 'function')
            throw 'Argument _errorCallback must be a function if defined';
        if (typeof arguments === 'function' && arguments.length > 1+2)
            throw 'Too many arguments ('+arguments.length+' instead of '+(1+2)+')';
        return json_call_ajax("NarrativeJobService.check_job",
            [job_id], 1, _callback, _errorCallback);
    };
  

    /*
     * JSON call using jQuery method.
     */
    function json_call_ajax(method, params, numRets, callback, errorCallback, json_rpc_context) {
        var deferred = $.Deferred();

        if (typeof callback === 'function') {
           deferred.done(callback);
        }

        if (typeof errorCallback === 'function') {
           deferred.fail(errorCallback);
        }

        var rpc = {
            params : params,
            method : method,
            version: "1.1",
            id: String(Math.random()).slice(2),
        };
        if (json_rpc_context)
            rpc['context'] = json_rpc_context;

        var beforeSend = null;
        var token = (_auth_cb && typeof _auth_cb === 'function') ? _auth_cb()
            : (_auth.token ? _auth.token : null);
        if (token != null) {
            beforeSend = function (xhr) {
                xhr.setRequestHeader("Authorization", token);
            }
        }

        var xhr = jQuery.ajax({
            url: _url,
            dataType: "text",
            type: 'POST',
            processData: false,
            data: JSON.stringify(rpc),
            beforeSend: beforeSend,
            timeout: _timeout,
            success: function (data, status, xhr) {
                var result;
                try {
                    var resp = JSON.parse(data);
                    result = (numRets === 1 ? resp.result[0] : resp.result);
                } catch (err) {
                    deferred.reject({
                        status: 503,
                        error: err,
                        url: _url,
                        resp: data
                    });
                    return;
                }
                deferred.resolve(result);
            },
            error: function (xhr, textStatus, errorThrown) {
                var error;
                if (xhr.responseText) {
                    try {
                        var resp = JSON.parse(xhr.responseText);
                        error = resp.error;
                    } catch (err) { // Not JSON
                        error = "Unknown error - " + xhr.responseText;
                    }
                } else {
                    error = "Unknown Error";
                }
                deferred.reject({
                    status: 500,
                    error: error
                });
            }
        });

        var promise = deferred.promise();
        promise.xhr = xhr;
        return promise;
    }
}


