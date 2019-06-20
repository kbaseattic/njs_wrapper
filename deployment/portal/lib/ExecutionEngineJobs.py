import logging
import time
from typing import Dict

from pymongo import collection

from lib.HTCondorWrapper import HTCondorWrapper
from lib.clients.NJSDatabaseClient import NJSDatabaseClient
from lib.clients.UJSDatabaseClient import UJSDatabaseClient
from lib.clients.feeds_client import feeds_client
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

    def log_incomplete_jobs(self, incomplete_jobs=None):
        """
        This function is for testing purposes
        :return:
        """
        if incomplete_jobs is None:
            incomplete_jobs = self.get_incomplete_jobs()

        logging.info(
            f"{len(incomplete_jobs.keys())} are actually incomplete")
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
        logging.info(f"Found {icc} incomplete_jobs (before checking their status in condor)")

        incomplete_jobs = {}

        # Store incomplete jobs
        for job_id in incomplete_ujs_jobs.keys():
            condor_job = condor_jobs.get(job_id,None)
            if condor_job is not None:
                if HTCondorWrapper.job_will_complete(condor_job):
                    continue

            incomplete_jobs[job_id] = condor_job
        return incomplete_jobs

    def generate_error_message(self, ujs_job=None, njs_job=None):
        if ujs_job is None:
            logging.error("Programming error")

        id = ujs_job['_id']
        username = ujs_job['user']
        wsid = ujs_job['authparam']

        message = f"Attn {ujs_job['user']}: Due to a system error on {ujs_job['created']}, job {ujs_job['_id']} has failed. We are very sorry for the inconvenience. Plese resubmit the job. "

        # TODO specific endpoint

        message += f" URL=https://narrative.kbase.us/narrative/ws.{wsid}"

        if njs_job is not None:

            if "app_id" in njs_job and njs_job["app_id"] is not None:
                message += f" AppID[{njs_job['app_id']}]"
            if "method" in njs_job and njs_job["method"] is not None:
                message += f" Method[{njs_job['method']}]"
            # if "wsid" in njs_job and njs_job["wsid"] is not None:

        return message

    def mark_job_as_purged(self, job_id, dry_run=True):
        if dry_run is True:
            logging.info(f"About to mark {job_id} as completed in ujs")
        else:
            self.ujs_db.mark_job_as_purged(job_id)
            self.log_purged_job(job_id)

    def log_purged_job(self, ):
        """
        Write to a file
        :return:
        """
        pass

    def purge_incomplete_jobs(self, dry_run=True):
        incomplete_jobs = self.get_incomplete_jobs()
        self.log_incomplete_jobs(incomplete_jobs=incomplete_jobs)

        njs_jobs = self.njs_db.get_jobs_by_ujs_ids(list(incomplete_jobs.keys()))

        messages = []
        fc = feeds_client.feeds_service_client()

        for job_id in incomplete_jobs.keys():
            app_name = None
            if job_id in njs_jobs.keys():
                njs_job = njs_jobs[job_id]
                message = (self.generate_error_message(ujs_job=incomplete_jobs[job_id],
                                                       njs_job=njs_job))

                if "app_id" in njs_job and njs_job["app_id"] is not None:
                    app_name = njs_job['app_id']

                messages.append(message)
            else:
                message = (self.generate_error_message(ujs_job=incomplete_jobs[job_id]))
                messages.append(message)

            user_name = incomplete_jobs[job_id]['user']
            fc.notify_users_workspace(user=user_name, message=message, job_id=job_id,
                                      dry_run=dry_run,
                                      app_name=app_name)

            self.mark_job_as_purged(job_id, dry_run=dry_run)

        send_slack_message("\n".join(messages))

        # TODO Create admin endpoint for logging in NJS and append info to end of job log

        # Send message to slack
        # Send message to feed
        # Mark as incomplete in UJS

        pass
