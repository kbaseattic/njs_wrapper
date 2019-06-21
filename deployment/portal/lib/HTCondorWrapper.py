import os
import sys
from typing import Dict

import htcondor
import enum


class HTCondorWrapper:
    class JobStatusCodes(enum.Enum):
        UNEXPANDED = 0
        IDLE = 1
        RUNNING = 2
        REMOVED = 3
        COMPLETED = 4
        HELD = 5
        SUBMISSION_ERROR = 6
        NOT_FOUND = -1

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

    @staticmethod
    def get_job_status(job):
        pass

    @staticmethod
    def job_will_complete(job):
        """
        If the job is Idle or Running, it is not incomplete
        If the job is Held, make sure the Hold Reason is not the HELD state from before
        """
        # Job is unexpanded idle or running, it will complete
        print(job)
        job_status = job.get("JobStatus", -1)
        job_name = job.get("JobBatchName")
        if job_status in [0, 1, 2]:
            print(
                f"This job will complete because its status is {job_status} {job_name}"
            )
            return True
        if job_status in [3, 4, 6, -1]:
            print(
                f"This job will NOT COMPLETE complete because its status is {job_status} {job_name}"
            )
            return False
        if job_status == HELD:
            job_hold_reason = job.get("HoldReason", None)
            if job_hold_reason is None:
                raise Exception("Something is wrong with job" + job.get("JobBatchName"))
            """
            if job hold reason is NOT the preemption hold thing
            return True
            """
            print(
                f"This HELD JOB will NOT COMPLETE complete because its status is {job_status} {job_name}"
            )

            return False

    @staticmethod
    def _check_if_not_root():
        if os.geteuid() == 0:
            sys.exit("Cannot run script as root. Need access to htcondor password file")

    @staticmethod
    def get_condor_q_jobs(requirements=None) -> Dict[str, Dict]:
        """
        Query the Schedd for all jobs currently stored in condor_q created by NJS
        The jobs are
        * Idle
        * Running
        * Held
        * Possibly Complete
        :param requirements: Condor Specific requirements (e.g. LastJobStatus == 1)
        :return: A dict of condor jobs keyed by UJS-JOB-ID
        """

        HTCondorWrapper._check_if_not_root()

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
