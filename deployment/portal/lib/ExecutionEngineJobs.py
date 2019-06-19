import logging
from typing import Dict
import datetime
import time

from pymongo import collection

from lib.HTCondorWrapper import HTCondorWrapper
from lib.clients.NJSDatabaseClient import NJSDatabaseClient
from lib.clients.UJSDatabaseClient import UJSDatabaseClient
from lib.utils import send_slack_message

logging.basicConfig(level=logging.DEBUG)


class ExecutionEngineJobs:

    def _find_incomplete_ujs_jobs(self) -> Dict[str, type(collection)]:
        # TODO USE PROJECTION
        """

        :return:
        """

        jobstate = self.ujs_db.get_jobs_collection()
        incomplete_jobs = list(jobstate.find({"complete": {"$ne": True}}))
        ij = {}
        for job in incomplete_jobs:
            ij[str(job['_id'])] = job
        return ij

    def __init__(self):
        self.incomplete_jobs = None
        self.njs_db = NJSDatabaseClient()
        self.ujs_db = UJSDatabaseClient()

    def log_incomplete_jobs(self):
        """
        This function is for testing purposes
        :return:
        """
        incomplete_jobs = self.get_incomplete_jobs()

        logging.info(
            f"Found {icc} incomplete jobs. Of these, {len(incomplete_jobs.keys())} are actually incomplete")
        now = time.time()

        dead_jobs_fp = f"{now}.{len(incomplete_jobs)}.dead_jobs"
        logging.info(f"Logged to {dead_jobs_fp}")
        with open(dead_jobs_fp, "w") as f:
            # f.writelines("\t".join(self.attributes) + "\n")
            f.writelines("\n".join(incomplete_jobs))

    def mark_job_as_error(self):
        self.log_incomplete_jobs()

    def get_incomplete_jobs(self):
        incomplete_ujs_jobs = self._find_incomplete_ujs_jobs()
        condor_jobs = HTCondorWrapper.get_condor_q_jobs()
        icc = len(incomplete_ujs_jobs.keys())
        print(f"Found {icc} incomplete_jobs")

        incomplete_jobs = {}

        # Store incomplete jobs
        for job_id in incomplete_ujs_jobs.keys():
            condor_job = incomplete_ujs_jobs[job_id]
            if job_id in condor_jobs:
                if HTCondorWrapper.job_will_complete(condor_job):
                    continue

            incomplete_jobs[job_id] = condor_job
        return incomplete_jobs

    def purge_incomplete_jobs(self):
        incomplete_jobs = self.get_incomplete_jobs()
        njs_jobs = self.njs_db.get_jobs_by_ujs_ids(incomplete_jobs.keys())



        for job_id in incomplete_jobs:
            if job_id in njs_jobs:
                print(job_id, njs_jobs[job_id])
            else:
                print(f"Job {job_id} not found in njs")

            #message = generateMessage()
            #send_slack_message(message)




        for job_id in incomplete_jobs():
            pass

            # Send message to slack
            # Send message to feed
            # Mark as incomplete in UJS




        pass
