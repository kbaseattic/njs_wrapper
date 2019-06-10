#!/usr/bin/env python
from .lib.njs_jobs import njs_jobs
from .lib.utils import send_slack_message


class Portal:
    def __init__(self):
        pass

    def process_event(self):
        pass

    def handle_job_hold_exit_fail(self, jobBatchName):
        job = njs_jobs()

        complete = job.is_job_complete(jobBatchName)
        if complete:
            msg = f"Job {jobBatchName} has been held. The job is marked as complete or error. Please investigate"
            send_slack_message(msg)
            pass
        else:
            njs_jobs.process_failed_job(jobBatchName)

    def handle_job_hold_exit_success(self):
        pass
