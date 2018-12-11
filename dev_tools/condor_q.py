#!/usr/bin/env python
import argparse
import json
import os
import re
import subprocess
import sys
from collections import defaultdict

job_status = {
    5: 'Held',
    4: 'Completed',
    3: 'Removed',
    2: 'Running',
    1: 'Idle',
    'U': 'Unexpanded',
    'E': 'Submission_error'
}



def separator(f):
    sep = "=" * 160
    def wrap(*args, **kwargs):
        print(sep)
        return f(*args, **kwargs)
    return wrap

# TODO: Cache this
def get_jobs_by_status(data):
    jobs = defaultdict(list)
    for item in data:
        jobstatus = job_status[item['JobStatus']]
        jobs[jobstatus].append(item)
    return jobs


def print_general_queue_status(jobs_by_status):
    for status in jobs_by_status:
        print("{}:{}".format(status, len(jobs_by_status[status])))


def get_client_group(job):
    try:
        return re.search('CLIENTGROUP == (".+?")', job['Requirements']).group(1).replace('"', '')
    except Exception:
        return 'unknown'

@separator
def print_job_status_by_status():
    print("Job Stats by Status")
    jobs_by_status = get_jobs_by_status(json_data)

    for status in jobs_by_status.keys():
        jobs = jobs_by_status[status]

        print("{}:{}".format(status, len(jobs_by_status[status])))

        cg = defaultdict(int)

        for job in jobs:
            if 'Requirements' in job:
                client_group = get_client_group(job)
                cg[client_group] += 1

        for c in cg:
            print("\t{}:{}".format(c, cg[c]))

@separator
def print_job_status_by_client_group():
    print("Job Stats by ClientGroup")
    jobs_by_status = get_jobs_by_status(json_data)

    cg = defaultdict(lambda: defaultdict(int))

    for status in jobs_by_status.keys():
        jobs = jobs_by_status[status]

        for job in jobs:
            if 'Requirements' in job:
                client_group = get_client_group(job)
                cg[client_group][status] += 1

    for c in cg:
        print("{}:{}".format(str(c), dict(cg[c])))


def load_condor_q_from_file(file):
    with open(file) as f:
        return json.load(f)


def look_for_condor_container():
    cmd = "docker ps | grep kbase/condor | cut -f1 -d' '"
    lines = subprocess.check_output(cmd, shell=True).decode().split("\n")

    names = []
    for line in lines:
        if len(line) > 2:
            names.append(line)

    if len(names) > 1:
        sys.exit("Found too many container names" + str(names))
    else:
        return str(names[0])


def load_condor_q():
    if cmd_exists('condor_q'):
        comm = ['condor_q', '-json']
        output = subprocess.check_output(comm, shell=True).decode()
        return json.loads(output)
    else:
        container_id = look_for_condor_container()
        comm = " ".join(['docker', 'exec', '-it', '-u', '0', container_id, 'condor_q', '-json'])
        output = subprocess.check_output(comm, shell=True).decode()
        return json.loads(output)


def cmd_exists(cmd):
    return any(
        os.access(os.path.join(path, cmd), os.X_OK)
        for path in os.environ["PATH"].split(os.pathsep)
    )


def print_table(table):
    longest_cols = [
        (max([len(str(row[i])) for row in table]) + 3)
        for i in range(len(table[0]))
    ]
    row_format = "".join(["{:>" + str(longest_col) + "}" for longest_col in longest_cols])
    for row in table:
        print(row_format.format(*row))

@separator
def display_job_info(status='Idle', fields=None):
    jobs_by_status = get_jobs_by_status(json_data)

    if fields is None:
        fields = ['JobBatchName', 'ClusterId', 'AcctGroup', 'RemoteHost', 'kb_app_id',
                  'LastRejMatchReason']

    print_lines = []
    for job in jobs_by_status[status]:
        line = []

        for field in fields:
            if field in job:
                line.append("{}".format(job[field]))
            else:
                line.append("")

        line.append(get_client_group(job))
        print_lines.append(line)

    fields.append("ClientGroup")

    print_lines.insert(0, fields)
    print(print_table(print_lines))


def display_idle_jobs():
    display_job_info('Idle')


def display_running_jobs():
    display_job_info('Running')


def display_removed_jobs():
    display_job_info('Removed')


def display_held_jobs():
    display_job_info('Held')


def display_completed_jobs():
    display_job_info('Completed')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='See info extracted from condor_q')
    parser.add_argument('--file', help='Specify output file from condor_q -json')
    parser.add_argument('--status', action='store_true', help='Get status of all jobs')
    parser.add_argument('--status_client_group', action='store_true',
                        help='Get status of all queues')
    parser.add_argument('--idle', action='store_true', help='Get status of all idle jobs')
    parser.add_argument('--running', action='store_true', help='Get status of all running jobs')
    parser.add_argument('--removed', action='store_true', help='Get status of all removed jobs')
    parser.add_argument('--held', action='store_true', help='Get status of all held jobs')
    parser.add_argument('--completed', action='store_true', help='Get status of all completed jobs')

    args = parser.parse_args()

    if args.file:
        json_data = load_condor_q_from_file(args.file)
    else:
        json_data = load_condor_q()

    if args.status:
        print_job_status_by_status()
    if args.status_client_group:
        print_job_status_by_client_group()
    if args.idle:
        display_idle_jobs()
    if args.running:
        display_running_jobs()
    if args.removed:
        display_removed_jobs()
    if args.held:
        display_held_jobs()
    if args.completed:
        display_completed_jobs()

#TODO Cache call to condor_q
#TODO Figure out why None is being printed
