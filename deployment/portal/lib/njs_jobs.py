#!/usr/bin/env python
import os
import sys
import time
from configparser import ConfigParser

import htcondor
from pymongo import MongoClient, database, collection

from .feeds_client import feeds_client

from typing import Dict, Tuple, Sequence, List
from .utils import send_slack_message


class njs_jobs:
    jsc = {
        "0": "Unexepanded",
        1: "Idle",
        2: "Running",
        3: "Removed",
        4: "Completed",
        5: "Held",
        6: "Submission_err",
        -1: "Not found in condor",
    }

    njs_jobs_collection = "exec_tasks"
    ujs_jobs_collection = "jobstate"


    @staticmethod
    def get_config() -> ConfigParser:
        parser = ConfigParser()
        parser.read(os.environ["KB_DEPLOYMENT_CONFIG"])
        return parser

    @staticmethod
    def get_ujs_connection() -> MongoClient:
        parser = njs_jobs.get_config()
        ujs_host = parser.get("NarrativeJobService", "ujs-mongodb-host")
        ujs_db = parser.get("NarrativeJobService", "ujs-mongodb-database")
        ujs_user = parser.get("NarrativeJobService", "ujs-mongodb-user")
        ujs_pwd = parser.get("NarrativeJobService", "ujs-mongodb-pwd")
        return MongoClient(
            ujs_host, 27017, username=ujs_user, password=ujs_pwd, authSource=ujs_db
        )

    @staticmethod
    def get_njs_connection() -> MongoClient:
        parser = njs_jobs.get_config()
        njs_host = parser.get("NarrativeJobService", "mongodb-host")
        njs_db = parser.get("NarrativeJobService", "mongodb-database")
        njs_user = parser.get("NarrativeJobService", "mongodb-user")
        njs_pwd = parser.get("NarrativeJobService", "mongodb-pwd")
        return MongoClient(
            njs_host, 27017, username=njs_user, password=njs_pwd, authSource=njs_db
        )

    @staticmethod
    def get_ujs_database() -> database:
        return njs_jobs.get_ujs_connection().get_database(
            njs_jobs.get_config().get("NarrativeJobService", "ujs-mongodb-database")
        )

    @staticmethod
    def get_njs_database() -> database:
        return njs_jobs.get_njs_connection().get_database(
            njs_jobs.get_config().get("NarrativeJobService", "mongodb-database")
        )

    @staticmethod
    def check_if_not_root():
        if os.geteuid() == 0:
            sys.exit("Cannot run script as root. Need access to htcondor password file")

    @staticmethod
    def get_njs_jobs(jobs) -> List[Dict]:
        jobstate = njs_jobs.get_njs_database().get_collection("exec_tasks")
        return list(
            jobstate.find(
                {"ujs_job_id": {"$in": jobs}},
                projection=["ujs_job_id", "job_input", "task_id"],
            )
        )

    @staticmethod
    def get_njs_job(job) -> List[Dict]:
        jobstate = njs_jobs.get_njs_database().get_collection("exec_tasks")
        return list(
            jobstate.find(
                {"ujs_job_id": {"$in": job}},
                projection=["ujs_job_id", "job_input", "task_id"],
            )
        )

    @staticmethod
    def get_njs_jobs_by_id(ujs_job_ids) -> Dict[str, Dict]:

        jobs_by_id = {}
        desired_input_fields = ["app_id", "wsid", "method"]

        for job in njs_jobs.get_njs_jobs(ujs_job_ids):
            job_id = job.get("ujs_job_id", None)
            if job_id is None:
                continue

            jobs_by_id[job_id] = {
                "ujs_job_id": job_id,
                "app_id": job["job_input"].get("app_id", None),
                "wsid": job["job_input"].get("wsid", None),
                "method": job["job_input"].get("method", None),
                "task_id": job.get("task_id", None),
            }

        return jobs_by_id

    def get_incomplete_jobs(self) -> List[Dict]:
        # TODO USE PROJECTION
        """

        :return:
        """
        if self.incomplete_jobs is None:
            jobstate = self.get_ujs_database().get_collection("jobstate")
            self.incomplete_jobs = list(jobstate.find({"complete": {"$ne": True}}))
        return self.incomplete_jobs

    @staticmethod
    def get_condor_jobs(requirements=None) -> Dict[str, Dict]:
        schedd = htcondor.Schedd()
        jobs = schedd.xquery(
            requirements=requirements,
            projection=[
                "JobBatchName",
                "JobStatus",
                "ClusterId",
                "RemoteHost",
                "LastRemoteHost",
                "HoldReason",
            ],
        )
        condor_jobs = {}

        for job in jobs:
            if "JobBatchName" not in job:
                continue
            job_id = job["JobBatchName"]
            condor_jobs[job_id] = job

        return condor_jobs

    @staticmethod
    def get_condor_jobs_all() -> Dict[str, Dict]:
        return njs_jobs.get_condor_jobs()

    @staticmethod
    def get_non_held_non_running_condor_jobs() -> Dict[str, Dict]:
        return njs_jobs.get_condor_jobs(requirements="JobStatus != 2 && JobStatus !=1")

    def __init__(self):
        self.check_if_not_root()

        # TODO Investigate if non authstrat exists
        # 'authstrat': 'kbaseworkspace', 'authparam': '28817'

        self.requested_job_attributes = [
            "_id",
            "task_id",
            "user",
            "wsid",
            "app_id",
            "method",
            "created",
            "updated",
            "desc",
            "status",
            "authparam",
            "condor_job_status",
            "condor_job_status_human",
        ]

        self.incomplete_jobs = None
        self.dead_jobs = None
        self._feeds_client = None

    def get_dead_jobs(self):
        """
        A dead job is a job that is "not complete", not "running (2)" and not "idle (1)"
        A dead job is also only a dead job if the status is "HELD (5)" ,
        but it depends on the HOLD REASON
        :return:
        """
        ujs_jobs = self.get_incomplete_jobs()

        condor_jobs = self.get_condor_jobs_all()
        incomplete_jobs = {}

        for job in ujs_jobs:
            job["_id"] = str(job["_id"])
            job_id = job["_id"]

            condor_job = condor_jobs.get(job_id, {})
            condor_job_status = condor_job.get("JobStatus", -1)
            if condor_job_status not in [1, 2]:
                incomplete_jobs[job_id] = job

        njs_jobs = self.get_njs_jobs_by_id(list(incomplete_jobs.keys()))

        for job_id in njs_jobs.keys():
            njs_job = njs_jobs[job_id]
            incomplete_jobs[job_id]["app_id"] = njs_job.get("app_id", None)
            incomplete_jobs[job_id]["wsid"] = njs_job.get("wsid", None)
            incomplete_jobs[job_id]["method"] = njs_job.get("method", None)
            incomplete_jobs[job_id]["task_id"] = njs_job.get("task_id", None)

        print("About to return dead jobs")
        for job_id in incomplete_jobs.keys():
            print(incomplete_jobs[job_id])

        return incomplete_jobs

    def report_all_condor_jobs(self):
        for j in self.get_condor_jobs_all():
            print(j)

    def report_potentially_dead_condor_jobs(self):
        for j in self.get_non_held_non_running_condor_jobs():
            print(j)

    def log_dead_jobs(self):
        self.dead_jobs = self.get_dead_jobs()

        now = time.time()
        with open(f"{now}.{len(self.dead_jobs)}.dead_jobs", "w") as f:
            # f.writelines("\t".join(self.attributes) + "\n")
            f.writelines("\n".join(self.dead_jobs))



    def purge_dead_jobs(self, dryrun=True):
        """
        Save logs of failed jobs marked as ERROR/CANCELED in DATABASE #TODO TRY THIS OUT IN BROWSER AND SEE WHAT HAPPENS
        Save logs of notifications (feeds) sent

        :param dryrun:
        :return:
        """
        now = time.time()
        if self.dead_jobs is None:
            self.log_dead_jobs()
        if self.dead_jobs is None:
            sys.exit("No dead jobs to purge " + now)

        notifications = []
        for job_id in self.dead_jobs.keys():
            self.set_job_to_error(job_id)
            note = self.notify_user(self.dead_jobs[job_id])
            notifications.append(note)

        send_slack_message("\n".join(notifications))

    def get_feeds_client(self):
        if self._feeds_client is None:
            self._feeds_client = feeds_client.feeds_service_client()
        return self._feeds_client

    def set_job_to_error(self, job):
        print(f"Setting {job} to error in UJS")
        pass

    def notify_user(self, job):
        user = job['user']
        job_id = job['_id']

        message = f"Attn {user}: Due to a system error on {job['created']}, job {job_id} has failed. We are very sorry for the inconvenience. Plese resubmit the job. "
        if "app_id" in job and job["app_id"] is not None:
            message += f" AppID[{job['app_id']}]"
        if "method" in job and job["method"] is not None:
            message += f" Method[{job['method']}]"
        if "wsid" in job and job["wsid"] is not None:
            message += f" URL=https://narrative.kbase.us/narrative/ws.{job['wsid']}"

        fc = self.get_feeds_client().notify_users_workspace()

        fc.notify_users_workspace(
            user=user, message=message, job_id=job_id, app_name=job["app_id"]
        )


        return message

    def notify_users(self, job):
        #
        pass

    #TODO Save Logs
    #TODO Create a little database so you don't send double feed messages
    #TODO Send the feed messages
    #TODO Mark the Database as Done
    #TODO Have a function that can read a log and resend messages based on that and database above
    #TODO Make a "Portal Function" to scan job log and spit out events
    #TODO Test out Holding Jobs (aka cgroups memory limit?) or just manually hold them and then resume them and look at LOGS and such
    #TODO Create a new database that combines NJS and UJS and then build an API around that
    #TODO REMOVE CLEANUP SCRIPT AND REPLACE IT WITH A PYHTHON CLEANUP SCRIPT

if __name__ == "__main__":
    m = njs_jobs()
    njs_jobs.log_dead_jobs()
    feeds_client = feeds_client.feeds_service_client()
