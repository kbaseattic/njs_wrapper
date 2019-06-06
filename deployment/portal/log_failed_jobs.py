#!/usr/bin/env python
import pymongo
import sys
import os
import os
import pwd
import htcondor
import requests
from bson.objectid import ObjectId
from configparser import ConfigParser
import time

seconds = time.time()

from pymongo import MongoClient, database, collection

from typing import List

jsc = {"0": "Unexepanded",
       1: "Idle",
       2: "Running",
       3: "Removed",
       4: "Completed",
       5: "Held",
       6: "Submission_err",
       -1: "Not found in condor"
       }


def get_config() -> ConfigParser:
    parser = ConfigParser()
    parser.read(os.environ['KB_DEPLOYMENT_CONFIG'])
    return parser


def get_ujs_connection() -> MongoClient:
    parser = get_config()
    ujs_host = parser.get('NarrativeJobService', 'ujs-mongodb-host')
    ujs_db = parser.get('NarrativeJobService', 'ujs-mongodb-database')
    ujs_user = parser.get('NarrativeJobService', 'ujs-mongodb-user')
    ujs_pwd = parser.get('NarrativeJobService', 'ujs-mongodb-pwd')
    return MongoClient(ujs_host, 27017, username=ujs_user, password=ujs_pwd, authSource=ujs_db)


def get_njs_connection() -> MongoClient:
    parser = get_config()
    njs_host = parser.get('NarrativeJobService', 'mongodb-host')
    njs_db = parser.get('NarrativeJobService', 'mongodb-database')
    njs_user = parser.get('NarrativeJobService', 'mongodb-user')
    njs_pwd = parser.get('NarrativeJobService', 'mongodb-pwd')
    return MongoClient(njs_host, 27017, username=njs_user, password=njs_pwd, authSource=njs_db)


def get_ujs_database() -> database:
    return get_ujs_connection().get_database(
        get_config().get('NarrativeJobService', 'ujs-mongodb-database'))


def get_njs_database() -> database:
    return get_ujs_connection().get_database(
        get_config().get('NarrativeJobService', 'mongodb-database'))


def check_if_not_root():
    if os.geteuid() == 0:
        sys.exit('Cannot run script as root. Need access to htcondor password file')


def get_incomplete_jobs() -> collection:
    jobstate = get_ujs_database().get_collection('jobstate')
    return jobstate.find({'complete': {'$ne': True}})


def get_condor_jobs_all():
    schedd = htcondor.Schedd()
    jobs = schedd.xquery(
        projection=['JobBatchName', 'JobStatus', 'ClusterId', 'RemoteHost',
                    'LastRemoteHost'])
    return jobs


def find_dead_jobs(incomplete_jobs, condor_jobs) -> List:
    return []


def create_report(dead_jobs: List):
    print(1)


def find_dead_jobs(jobs, condor_jobs, attributes):
    cj = {}
    for j in condor_jobs:
        cj[j['JobBatchName']] = j['JobStatus']

    dead_jobs = []

    for job in jobs:
        row = ([str(job.get(x, None)) for x in attributes])
        id = row[0]
        if id in cj.keys():
            row.append(cj[id])
        else:
            row.append(-1)

        status = row[-1]
        row.append(jsc[status])
        if (status != 2):
            print(row)
            dead_jobs.append("\t".join(str(ele) for ele in row))

    return dead_jobs


def record_dead_jobs(jobs, attributes):
    now = time.time()
    with open(f"{now}.dead_jobs", "w") as f:
        f.writelines("\t".join(attributes) + "\n")
        f.writelines("\n".join(jobs))


if __name__ == '__main__':
    check_if_not_root()
    incomplete_jobs = get_incomplete_jobs()
    data_points = ["_id", "user", "created", "updated", "desc", "status", ]

    condor_jobs = get_condor_jobs_all()
    dead_jobs = find_dead_jobs(incomplete_jobs, condor_jobs, data_points)
    record_dead_jobs(dead_jobs, data_points)




# def update_task(job_id, field, value):
#     ujs = client.get_database(ujs_db)
#     jobstate = ujs.get_collection('jobstate')
#     return jobstate.find_one({'_id': ObjectId(job_id)})
