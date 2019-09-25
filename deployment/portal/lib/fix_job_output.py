from pymongo import MongoClient, database, collection
from configparser import ConfigParser
import os

class NJSDatabaseClient:
    def __init__(self):
        self.parser = self._get_config()
        self.njs_db_name = self.parser.get("NarrativeJobService", "mongodb-database")
        self.njs_jobs_collection_name = "exec_tasks"
        self.njs_logs_collection_name = "exec_logs"

    def _get_njs_connection(self) -> MongoClient:
        parser = self.parser
        njs_host = parser.get("NarrativeJobService", "mongodb-host")
        njs_db = parser.get("NarrativeJobService", "mongodb-database")
        njs_user = parser.get("NarrativeJobService", "mongodb-user")
        njs_pwd = parser.get("NarrativeJobService", "mongodb-pwd")
        return MongoClient(
            njs_host, 27017, username=njs_user, password=njs_pwd, authSource=njs_db, retryWrites=False
        )

    def _get_jobs_database(self) -> database:
        return self._get_njs_connection().get_database(self.njs_db_name)

    def get_jobs_collection(self) -> collection:
        return self._get_jobs_database().get_collection(self.njs_jobs_collection_name)


    @staticmethod
    def _get_config() -> ConfigParser:
        parser = ConfigParser()
        parser.read(os.environ.get("KB_DEPLOYMENT_CONFIG"))
        return parser

    def run(self,dryRun=True):
        f = open("jobs.txt")
        jobs = f.readlines()
        jobs_collection = self.get_jobs_collection()


        for job_id in jobs:
            job_id = job_id.strip()

            j = jobs_collection.find_one({'ujs_job_id' : job_id})
            if j is None:
                print("Skipping",job_id)
                continue
            else:
                print(
                    "About to make njs record job_output from null to {} for [" + str(job_id) + "]")
            print(j['job_output'])
            if dryRun is False:
                jobs_collection.find_one_and_update({'ujs_job_id' : job_id}, {"$set" : {'job_output' : {}}})



n = NJSDatabaseClient()
n.run(dryRun=True)
