from typing import List,Dict
from lib.clients.NJSDatabaseClient import NJSDatabaseClient
from lib.clients.UJSDatabaseClient import UJSDatabaseClient

class ExecutionEngineJobs:
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


    def _find_incomplete_jobs(self) -> List[Dict]:
        # TODO USE PROJECTION
        """

        :return:
        """
        if self.incomplete_jobs is None:
            jobstate = self.ujs_db.get_jobs_collection()
            self.incomplete_jobs = list(jobstate.find({"complete": {"$ne": True}}))
        return self.incomplete_jobs

    def __init__(self):
        self.incomplete_jobs = None
        self.njs_db = NJSDatabaseClient()
        self.ujs_db = UJSDatabaseClient()



    def log_incomplete_jobs(self):
        self.incomplete_jobs = self._find_incomplete_jobs()
        for job in self.incomplete_jobs:
            print(job)

        # for job in incomplete_jobs():
        #     writeLogFile

    def find_incomplete_jobs(self):
        self.log_incomplete_jobs()


    def purge_incomplete_jobs(self):
        pass



