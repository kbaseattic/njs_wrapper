#!/usr/bin/env python
import os
import sys
import time
from configparser import ConfigParser
from typing import Dict, List

from bson.objectid import ObjectId
from pymongo import MongoClient, database, collection

from .NarrativeJobServiceClient import NarrativeJobService
from .feeds_client import feeds_client
from .utils import send_slack_message


# TODO Should I be closing mongo connections to prevent a memory leak?
# TODO should I make the staticmethods stateful instead?
# TODO USE ORM?


class NJSDatabaseClient:

    def __init__(self):
        self.njs_db_name = self._get_config().get("NarrativeJobService", "mongodb-database")
        self.njs_jobs_collection_name = "exec_tasks"
        self.njs_logs_collection_name = "exec_logs"

    def _get_config(self) -> ConfigParser:
        if self.parser is None:
            self.parser = ConfigParser()
            self.parser.read(os.environ["KB_DEPLOYMENT_CONFIG"])
        return self.parser

    def _get_njs_connection(self) -> MongoClient:
        parser = self._get_config()
        njs_host = parser.get("NarrativeJobService", "mongodb-host")
        njs_db = parser.get("NarrativeJobService", "mongodb-database")
        njs_user = parser.get("NarrativeJobService", "mongodb-user")
        njs_pwd = parser.get("NarrativeJobService", "mongodb-pwd")
        return MongoClient(
            njs_host, 27017, username=njs_user, password=njs_pwd, authSource=njs_db
        )

    def _get_jobs_database(self) -> database:
        return self._get_njs_connection().get_database(self.njs_db_name)

    def _get_jobs_collection(self) -> collection:
        return self._get_jobs_database().get_collection(self.njs_jobs_collection_name)

    def _get_logs_collection(self) ->collection:
        return self._get_jobs_database().get_collection(self.njs_logs_collection_name)

    def get_jobs(self, job_ids, projection=None) -> List[Dict]:
        return [self._get_jobs_collection().find({"ujs_job_id": {"$in": job_ids}}, projection=projection)]

    def get_logs(self, job_ids, projection=None) -> List[Dict]:
        return [self._get_jobs_collection().find({"ujs_job_id": {"$in": job_ids}}, projection=projection)]

    def get_job(self, job_id, projection=None) -> List[Dict]:
        return [self._get_jobs_collection().find({"ujs_job_id": {"$eq": job_id}}, projection=projection)]

    def get_log(self, job_id, projection=None) -> List[Dict]:
        return [self._get_jobs_collection().find({"ujs_job_id": {"$eq": job_id}}, projection=projection)]

    def replace_log(self,job_id,replacement_document):
        self._get_jobs_collection().replace_one(filter={"ujs_job_id": {"$eq": job_id}}, replacement=replacement_document)




if __name__ == "__main__":
    ndc = NJSDatabaseClient()
    ujs_job_id = "565f5b6ee4b0a6527cead4a2"
    ndc.get_job(ujs_job_id)
    print(ndc)