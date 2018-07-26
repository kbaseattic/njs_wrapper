#!/usr/bin/env python
import argparse
import subprocess
import os
import shutil

try:
    from ConfigParser import ConfigParser  # py2
except:
    from configparser import ConfigParser  # py3

# ENV Variable Order of priority (descending)
# 1 Configuration File
# 2 Environmental Variables
# 3 Defaults

# ENV VARIABLES


optional_variables = ['CALLBACK_INTERFACE', 'REFDATA_DIR', 'AWE_CLIENTGROUP', 'MINI_KB', 'HOSTNAME']
resource_variables = ['request_cpus', 'request_memory', 'request_disk']
ENV = os.environ


class UnknownEnvironmentException(Exception):
    pass


class MissingRequiredEnvironmentalVariableException(Exception):
    pass


class MissingRequiredConfigVariable(Exception):
    pass


class MissingConfigurationFileException(Exception):
    pass


class UnknownKbaseEnvironment(Exception):
    pass


def check_required_config_options(config):
    required_config_options = ['KBASE_RUN_DIR', 'SCRATCH', 'BASE_DIR', 'REF_DATA_DIR',
                               'CALLBACK_INTERFACE']
    for item in required_config_options:
        if config.get("default", item) is None:
            raise MissingRequiredConfigVariable(item)


def create_directory(directory):
    try:
        os.mkdir(directory)
    except OSError as e:
        if e.errno is not 17:
            raise IOError("Couldn't create directory:" + directory)


def check_if_exists(directory):
    if not os.path.exists(directory):
        raise IOError(directory + " does not exist")


def ensure_exists(*items_to_check):
    for item in items_to_check:
        check_if_exists(item)


def get_config(kbase_environment):
    """
    Look for config in the format of $KB_JOB_CONFIG/[ci,appdev,next,prod].cfg
    :param kbase_environment: ['ci','next','appdev','prod']
    :return: config file or None
    """
    config_directory = ENV.get('KB_JOB_CONFIG', None)
    config_file_path = "{}/{}.cfg".format(config_directory, kbase_environment)

    if os.path.isfile(config_file_path):
        print("Reading config: " + config_file_path)
        config_parser = ConfigParser()
        config_parser.read(config_file_path)
        return config_parser


def set_basedir(config, ujs_job_id):
    """
    Examples include
        /scratch/kbaserun/UJS_JOB_ID
        /scratch/kbaserun/USERNAME/UJS_JOB_ID
    :param config: config file
    :param ujs_job_id: userjobstate job id
    :return: base_dir
    """
    base_dir = config.get('default', 'BASE_DIR')
    if ENV.get('USERNAME'):
        base_dir = "{}/{}".format(base_dir, ENV.get('USERNAME'))
    return "{}/{}".format(base_dir, ujs_job_id)


def set_refdata(config, kbase_environment):
    """
    Examples include
        refdata/
        refdata/kbase_environment
        refdata/ref_data_suffix or refdata/kbase_environment/ref_data_suffix
    :param config: config file
    :param kbase_environment: ['ci','next','appdev','prod', ]
    :return:  refdata_dir
    """
    refdata_dir = config.get('default', 'REF_DATA_DIR')

    if config.getboolean('default', "DEPLOY") is True:
        refdata_dir = "{}/{}".format(refdata_dir, kbase_environment)

    if config.get('default', 'REF_DATA_DIR_SUFFIX'):
        refdata_dir = "{}/{}".format(refdata_dir, config.get('default', 'REF_DATA_DIR_SUFFIX'))

    return refdata_dir


def check_required_variables():
    """
    Check to see if variables are correctly set
    :return: None
    """
    required_variables = ['KB_AUTH_TOKEN', 'BASE_DIR', 'KB_ADMIN_AUTH_TOKEN']
    for item in required_variables:
        env_var = ENV.get(item, None)
        if not env_var:
            raise MissingRequiredEnvironmentalVariableException(item)


def stage_required_files(destination=ENV['BASE_DIR']):
    """

    :param destination:
    :return: location of staged jar file
    """
    jar = 'NJSWrapper-all.jar'
    jar_destination = "{}/{}".format(destination, jar)
    mydocker_script = 'mydocker'
    mydocker_destination = "{}/{}".format(destination, mydocker_script)
    shutil.move(jar, jar_destination)
    shutil.move(mydocker_script, mydocker_destination)
    return jar_destination

def cleanup(jar_destination):
    jar = 'NJSWrapper-all.jar'
    mydocker_script = 'mydocker'
    os.unlink(jar)
    os.unlink(mydocker_script)
    os.unlink(jar_destination)






def get_kbase_env(njs_endpoint):
    """
    Get the environment prefix section of the njs endpoint
    :param njs_endpoint:
    :return: ['ci','next','appdev','prod']
    """
    # For Supported Environments
    valid_prefixes = ['ci', 'next', 'appdev', 'prod']
    try:
        prefix = njs_endpoint.split('.')[0].split('//')[1]
    except:
        raise UnknownKbaseEnvironment(
            "NJS_ENDPOINT is not valid: " + njs_endpoint + " must be of format http://prefix.kbase.us/njs_wrapper")
    if prefix not in valid_prefixes:
        if njs_endpoint == "https://kbase.us/services/njs_wrapper":
            return 'prod'
        err_msg = "\nNJS Endpoint [{}] is not recognized or prefix [{}] is not valid. \n Valid prefixes must be in format [{}] ".format(
            njs_endpoint, prefix, ",".join(valid_prefixes))
        raise UnknownKbaseEnvironment(err_msg)
    return prefix


def kbase_setup(ujs_job_id):
    """
    Setup for running on kbase servers, just need to ensure base_dir exists
    :param ujs_job_id:
    :return:
    """
    base_dir = "{}/{}".format(ENV['BASE_DIR'], ujs_job_id)
    create_directory(base_dir)
    ensure_exists(base_dir)
    ENV['BASE_DIR'] = base_dir


def config_based_setup(config, ujs_job_id, kbase_environment):
    """
    Config Variables To Send As Is = ['USE_SHIFTER','HOME','SCRATCH','CALLBACK_INTERFACE']
    Config Variablse To Send Modified ['DEPLOY','REF_DATA','BASE_DIR']
    :param config:
    :param ujs_job_id:
    :param njs_endpoint:
    :param kbase_environment:
    :return:
    """
    check_required_config_options(config)

    if config.get('default', 'DEPLOY') is True:
        ENV['DEPLOY'] = kbase_environment
    ENV['REF_DATA'] = set_refdata(config, kbase_environment)
    ENV['BASE_DIR'] = set_basedir(config, ujs_job_id)

    exportable_config_options = ['USE_SHIFTER', 'HOME', 'SCRATCH', 'CALLBACK_INTERFACE']
    for option in exportable_config_options:
        ENV[option] = config.get('default', option)

    create_directory(ENV['BASE_DIR'])
    ensure_exists(ENV['REF_DATA'], ENV['BASE_DIR'])


def setup(ujs_job_id, njs_endpoint):
    """
    Setup for launching the jobs
    :param ujs_job_id: UJS Job ID
    :param njs_endpoint: Endpoint such as https://ci.kbase.us/services/njs_wrapper
    :return:
    """
    kbase_environment = get_kbase_env(njs_endpoint)
    config = get_config(kbase_environment)
    check_required_variables()
    if config is None:
        kbase_setup(ujs_job_id)
    else:
        config_based_setup(config, ujs_job_id, kbase_environment)


def setup_mini_kb(ujs_job_id):
    ENV['MINI_KB'] = 'true'
    base_dir = "{}/{}".format(ENV['BASE_DIR'], ujs_job_id)
    create_directory(base_dir)
    ensure_exists(base_dir)
    ENV['BASE_DIR'] = base_dir


def launch_job(ujs_job_id, njs_endpoint):
    """
    Launch SDKLMR Jobs
    :param ujs_job_id: UJS Job ID
    :param njs_endpoint: Endpoint such as https://ci.kbase.us/services/njs_wrapper
    :return:
    """
    # Add all transferred files to path
    with open("env", "w+") as env:
        env.write(os.environ.items().__str__())

    mini_kb = True
    if not mini_kb:
        setup(ujs_job_id, njs_endpoint)
    else:
        setup_mini_kb(ujs_job_id)

    jar_path = stage_required_files()

    sdk_lmr = 'us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner'
    job_commands = ['/usr/bin/java', '-cp', jar_path, sdk_lmr, ujs_job_id, njs_endpoint, ]

    print(" ".join(job_commands))

    # Overwrite Env Variables
    for key, val in ENV.items():
        os.environ[key] = val

    out_file = ENV['BASE_DIR'] + "/" + 'sdk_lmr.out'
    err_file = ENV['BASE_DIR'] + "/" + 'sdk_lmr.err'

    print("Writing logs to: \n{}\n{} ".format(out_file, err_file))

    with open(out_file, 'w+') as out, open(err_file, 'w+') as err:
        subprocess.call(" ".join(job_commands), stdout=out, stderr=err, shell=True,
                        cwd=ENV['BASE_DIR'], executable='/bin/bash')

    #cleanup(jar_path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Launch SDKLocalMethodRunner jobs")
    parser.add_argument('ujs_job_id', help='ujs job id to run')
    parser.add_argument('njs_endpoint', help='njs endpoint')
    args = parser.parse_args()
    launch_job(args.ujs_job_id, args.njs_endpoint)
