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
from feeds_client import feeds_service_client

from pymongo import MongoClient, database, collection

from typing import List


class njs_jobs():
    jsc = {"0": "Unexepanded",
           1: "Idle",
           2: "Running",
           3: "Removed",
           4: "Completed",
           5: "Held",
           6: "Submission_err",
           -1: "Not found in condor"
           }

    @staticmethod
    def get_config() -> ConfigParser:
        parser = ConfigParser()
        parser.read(os.environ['KB_DEPLOYMENT_CONFIG'])
        return parser

    @staticmethod
    def get_ujs_connection(self) -> MongoClient:
        parser = self.get_config()
        ujs_host = parser.get('NarrativeJobService', 'ujs-mongodb-host')
        ujs_db = parser.get('NarrativeJobService', 'ujs-mongodb-database')
        ujs_user = parser.get('NarrativeJobService', 'ujs-mongodb-user')
        ujs_pwd = parser.get('NarrativeJobService', 'ujs-mongodb-pwd')
        return MongoClient(ujs_host, 27017, username=ujs_user, password=ujs_pwd, authSource=ujs_db)

    @staticmethod
    def get_njs_connection(self) -> MongoClient:
        parser = self.get_config()
        njs_host = parser.get('NarrativeJobService', 'mongodb-host')
        njs_db = parser.get('NarrativeJobService', 'mongodb-database')
        njs_user = parser.get('NarrativeJobService', 'mongodb-user')
        njs_pwd = parser.get('NarrativeJobService', 'mongodb-pwd')
        return MongoClient(njs_host, 27017, username=njs_user, password=njs_pwd, authSource=njs_db)

    @staticmethod
    def get_ujs_database(self) -> database:
        return self.get_ujs_connection().get_database(
            self.get_config().get('NarrativeJobService', 'ujs-mongodb-database'))

    @staticmethod
    def get_njs_database(self) -> database:
        return self.get_ujs_connection().get_database(
            self.get_config().get('NarrativeJobService', 'mongodb-database'))

    @staticmethod
    def check_if_not_root():
        if os.geteuid() == 0:
            sys.exit('Cannot run script as root. Need access to htcondor password file')

    @staticmethod
    def get_incomplete_jobs(self) -> collection:
        jobstate = self.get_ujs_database().get_collection('jobstate')
        return jobstate.find({'complete': {'$ne': True}})

    @staticmethod
    def get_condor_jobs_all():
        schedd = htcondor.Schedd()
        jobs = schedd.xquery(
            projection=['JobBatchName', 'JobStatus', 'ClusterId', 'RemoteHost',
                        'LastRemoteHost', 'HoldReason'])
        return jobs

    def find_dead_jobs(self, jobs, condor_jobs, attributes):
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
            row.append(self.jsc[status])
            if (status != 2):
                print(row)
                dead_jobs.append("\t".join(str(ele) for ele in row))

        return dead_jobs

    def __init__(self):
        self.check_if_not_root()
        self.incomplete_jobs = self.get_incomplete_jobs()
        self.attributes = ["_id", "user", "created", "updated", "desc", "status", ]
        self.condor_jobs = self.get_condor_jobs_all()
        self.dead_jobs = self.find_dead_jobs(self.incomplete_jobs, self.condor_jobs,
                                             self.attributes)

    def log_dead_jobs(self):
        now = time.time()
        # with open(f"{now}.dead_jobs", "w") as f:
        #     f.writelines("\t".join(self.attributes) + "\n")
        #     f.writelines("\n".join(dead_jobs))
        print("\t".join(self.attributes) + "\n")
        print("\n".join(self.dead_jobs))

    def purge_dead_jobs(self):
        pass


if __name__ == '__main__':
    m = njs_jobs()
    njs_jobs.log_dead_jobs()
    feeds_client = feeds_service_client()


