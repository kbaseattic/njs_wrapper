#!/usr/bin/env python
import os
from configparser import ConfigParser
from typing import Dict, List

from bson import ObjectId
from pymongo import MongoClient, database, collection, cursor


# TODO Should I be closing mongo connections to prevent a memory leak?
# TODO should I make the staticmethods stateful instead?
# TODO USE ORM?
# TODO DELETE UJS AND DEPRECATE THIS


class UJSDatabaseClient:
    def __init__(self):
        self.parser = self._get_config()
        self.njs_db_name = self.parser.get(
            "NarrativeJobService", "ujs-mongodb-database"
        )
        self.ujs_jobs_collection = "jobstate"

    @staticmethod
    def _get_config() -> ConfigParser:
        parser = ConfigParser()
        parser.read(os.environ.get("KB_DEPLOYMENT_CONFIG"))
        return parser

    def _get_ujs_connection(self) -> MongoClient:
        parser = self.parser
        ujs_host = parser.get("NarrativeJobService", "ujs-mongodb-host")
        ujs_db = parser.get("NarrativeJobService", "ujs-mongodb-database")
        ujs_user = parser.get("NarrativeJobService", "ujs-mongodb-user")
        ujs_pwd = parser.get("NarrativeJobService", "ujs-mongodb-pwd")
        return MongoClient(
            ujs_host, 27017, username=ujs_user, password=ujs_pwd, authSource=ujs_db
        )

    def mark_job_as_purged(self, job_id):
        filter = {"_id": ObjectId(job_id)}
        update = {"$set": {"complete": True, "error": True}}
        self.get_jobs_collection().update_one(filter=filter, update=update)

    def _get_jobs_database(self) -> database:
        return self._get_ujs_connection().get_database(self.njs_db_name)

    def get_jobs_collection(self) -> collection:
        return self._get_jobs_database().get_collection(self.ujs_jobs_collection)

    def get_jobs(self, job_ids: List, projection=None) -> cursor:
        object_ids = []
        for job_id in job_ids:
            object_ids.append(ObjectId(job_id))

        return [
            self.get_jobs_collection().find(
                {"_id": {"$in": object_ids}}, projection=projection
            )
        ]

    def get_job(self, job_id: str, projection=None) -> Dict:
        return self.get_jobs_collection().find_one(
            {"_id": {"$eq": ObjectId(job_id)}}, projection=projection
        )
