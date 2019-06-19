import logging
from typing import Dict

from pymongo import collection as pymongo_collection

from HTCondorWrapper import HTCondorWrapper
from lib.clients.NJSDatabaseClient import NJSDatabaseClient
from lib.clients.UJSDatabaseClient import UJSDatabaseClient


class ExecutionEngineJobs:

    def _find_incomplete_ujs_jobs(self) -> Dict[str, pymongo_collection]:
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
        incomplete_ujs_jobs = self._find_incomplete_ujs_jobs()
        condor_jobs = HTCondorWrapper.get_condor_q_jobs()
        icc = len(incomplete_ujs_jobs.keys())

        incomplete_jobs = {}

        # Store incomplete jobs
        for job_id in incomplete_jobs.keys():
            condor_job = incomplete_jobs[job_id]
            if job_id in condor_jobs:
                if HTCondorWrapper.job_will_complete(condor_job):
                    continue
                incomplete_jobs[job_id] = condor_job

        logging.info(
            f"Found {icc} incomplete jobs. Of these, {len(incomplete_jobs.keys())} are actually incomplete")



    def find_incomplete_jobs(self):
        self.log_incomplete_jobs()

    def purge_incomplete_jobs(self):
        pass
