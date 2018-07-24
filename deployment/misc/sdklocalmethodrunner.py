#!/usr/bin/env python
import argparse
import subprocess
import os

try:
    from ConfigParser import ConfigParser  # py2
except:
    from configparser import ConfigParser  # py3

# ENV Variable Order of priority (descending)
# 1 Configuration File
# 2 Environmental Variables
# 3 Defaults

# ENV VARIABLES

required_variables = ['KB_AUTH_TOKEN']
optional_variables = ['CALLBACK_INTERFACE', 'REFDATA_DIR', 'AWE_CLIENTGROUP', 'MINI_KB', 'HOSTNAME']
resource_variables = ['request_cpus', 'request_memory', 'request_disk']
all_variables = required_variables + optional_variables + resource_variables


class UnknownEnvironmentException(Exception):
    pass


class MissingRequiredEnvironmentalVariableException(Exception):
    pass


def get_known_profiles():
    """
    Unused currently
    :return:
    """
    return ['kbase', 'nersc', 'cori']


def detect_profile_from_environment():
    """
    Unused currently
    :return:
    """
    cluster_name_file = '/etc/clustername'

    if os.path.isfile(cluster_name_file):
        with open(cluster_name_file, 'r') as cn:
            profile = cn.read()
            if profile in get_known_profiles():
                return profile
            else:
                raise UnknownEnvironmentException(profile)

    kbase_image_filepath = '/kb/deployment/jettybase'
    if os.path.exists(kbase_image_filepath):
        return 'kbase'

    raise UnknownEnvironmentException('Unknown Environment')


def overwrite_env_with_config():
    """

    :return:
    """
    default_config_location = 'config.ini'

    if os.path.isfile(default_config_location):
        parser = ConfigParser()
        parser.read(default_config_location)

        for key, value in parser.items('config'):
            os.environ[key] = value



def create_directory(directory):
    try:
        os.mkdir(directory)
    except OSError as e:
        if e.errno is not 17:
            raise IOError("Couldn't create scratch directory:" + directory)

def cori_setup(ujs_job_id, njs_endpoint):
    """

    :param ujs_job_id:
    :param njs_endpoint:
    :return:
    """
    cori_variables = ['USE_SHIFTER', 'KBASE_RUN_DIR', 'REFDATA_DIR', 'SCRATCH', 'USERNAME']
    for item in cori_variables:
        if os.environ.get(item, None) is None:
            raise MissingRequiredEnvironmentalVariableException(item)

    # Add all transferred files to path
    os.environ['PATH'] += ':' + os.getcwd()

    # Determine correct refdata oath
    prefix = njs_endpoint.split('.')[0].split('//')[1]
    kbase_run_dir = os.environ['KBASE_RUN_DIR']
    os.environ['REFDATA_DIR'] = '{}/refdata/{}/refdata'.format(kbase_run_dir, prefix)

    # Ensure writeable directory exists
    scratch = os.environ['SCRATCH']
    writeable = "{}/writeable".format(scratch)
    create_directory(writeable)

    # Create job working directory
    username = os.environ['USERNAME']
    base_dir = "{}/{}".format(scratch, username, ujs_job_id)
    create_directory(base_dir)

    os.environ['BASE_DIR'] = base_dir


def kbase_setup(ujs_job_id):
    """

    :param ujs_job_id:
    :return:
    """
    kbase_variables = ['BASE_DIR']
    for item in kbase_variables:
        if os.environ.get(item, None) is None:
            raise MissingRequiredEnvironmentalVariableException(item)
    base_dir = "{}/{}".format(os.environ['BASE_DIR'], ujs_job_id)
    create_directory(base_dir)

    os.environ['BASE_DIR'] = base_dir


def launchjob(ujs_job_id, njs_endpoint):
    """

    :param ujs_job_id:
    :param njs_endpoint:
    :return:
    """
    host_type = os.environ.get('HOST_MACHINE_TYPE', None)

    if (host_type and host_type == 'cori'):
        cori_setup(ujs_job_id, njs_endpoint)
    elif (host_type and host_type == 'other'):
        pass
    else:
        kbase_setup(ujs_job_id)

    jar = 'NJSWrapper-all.jar'
    if(os.path.exists(jar)):
        jar_location = os.path.abspath(jar)
    else:
        raise IOError("Can't find jar: " + jar)

    print(jar_location)

    job_commands = ['java', '-cp', jar_location,
                    'us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner', ujs_job_id,
                    njs_endpoint, ]

    with open('sdk_lmr.out', 'w') as out, open('sdk_lmr.err', 'w') as err:
        subprocess.call(job_commands,  stdout=out, stderr=err, shell=True, cwd=os.environ['BASE_DIR'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Launch SDKLocalMethodRunner jobs")
    parser.add_argument('ujs_job_id', help='ujs job id to run')
    parser.add_argument('njs_endpoint', help='njs endpoint')
    args = parser.parse_args()

    for item in required_variables:
        env_var = os.environ.get(item, None)
        if not env_var:
            raise MissingRequiredEnvironmentalVariableException(item)

    overwrite_env_with_config()

    launchjob(args.ujs_job_id, args.njs_endpoint)
