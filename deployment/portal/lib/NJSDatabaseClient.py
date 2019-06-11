#!/usr/bin/env python
import os
from configparser import ConfigParser
from typing import Dict

from pymongo import MongoClient, database, collection, cursor


# TODO Should I be closing mongo connections to prevent a memory leak?
# TODO should I make the staticmethods stateful instead?
# TODO USE ORM?


class NJSDatabaseClient:

    def __init__(self):
        self.parser = self._get_config()
        self.njs_db_name = self.parser.get("NarrativeJobService", "mongodb-database")
        self.njs_jobs_collection_name = "exec_tasks"
        self.njs_logs_collection_name = "exec_logs"

    def _get_config(self) -> ConfigParser:
        parser = ConfigParser()
        parser.read(os.environ["KB_DEPLOYMENT_CONFIG"])
        return parser

    def _get_njs_connection(self) -> MongoClient:
        parser = self.parser
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

    def _get_logs_collection(self) -> collection:
        return self._get_jobs_database().get_collection(self.njs_logs_collection_name)

    def get_jobs(self, job_ids, projection=None) -> cursor:
        return [self._get_jobs_collection().find({"ujs_job_id": {"$in": job_ids}},
                                                 projection=projection)]

    def get_logs(self, job_ids, projection=None) -> cursor:
        return [self._get_jobs_collection().find({"ujs_job_id": {"$in": job_ids}},
                                                 projection=projection)]

    def get_job(self, job_id, projection=None) -> Dict:
        return self._get_jobs_collection().find_one({"ujs_job_id": {"$eq": job_id}},
                                                    projection=projection)

    def get_log(self, job_id, projection=None) -> Dict:
        return self._get_logs_collection().find_one({"ujs_job_id": {"$eq": job_id}},
                                                    projection=projection)

    def replace_log(self, job_id, replacement_document):
        self._get_jobs_collection().replace_one(filter={"ujs_job_id": {"$eq": job_id}},
                                                replacement=replacement_document)

    # def update_log(self, job_id, message):
    #     last_log_line = self.get_log(job_id)['lines'][-1]
    #     filter = {"ujs_job_id": {"$eq": job_id}}
    #     update =  {"$set": { "$arrayElemAt" : ["$lines", -1] } }
    #
    #     self._get_jobs_collection().
    #
    #     self._get_jobs_collection().update_one(filter=filter,
    #                                             update=update)

    def pop_last_log_element(self, job_id):
        filter = {"ujs_job_id": {"$eq": job_id}}
        popCommand = {"$pop": {"lines": 1}}
        pop = self._get_logs_collection().update_one(filter=filter, update=popCommand)
        print(pop.raw_result)

    def push_last_log_element(self, job_id, line_object):
        filter = {"ujs_job_id": {"$eq": job_id}}
        pushCommand = {"$push": {"lines": line_object}}
        push = self._get_logs_collection().update_one(filter=filter, update=pushCommand)
        print(push.raw_result)

    #It would be great to use
    #           - `array_filters` (optional): A list of filters specifying which
    #        array elements an update should apply. Requires MongoDB 3.6+.

    def update_last_log_line(self, job_id, message):
        last_log_line = self.get_log(job_id)['lines'][-1]
        last_log_line["line"] += f" {message}\n "
        last_log_line["is_error"] = True

        self.get_log(job_id)
        self.pop_last_log_element(job_id)
        self.push_last_log_element(job_id, last_log_line)
        self.get_log(job_id)