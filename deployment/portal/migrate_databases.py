#!/usr/bin/env python
import datetime
import sys

from memory_profiler import profile

from lib.clients.NJSDatabaseClient import NJSDatabaseClient
from lib.clients.UJSDatabaseClient import UJSDatabaseClient


class MigrateDatabases:
    """
    GET UJS Record, Get corresponding NJS record, combine the two and save them in a new
    collection, using the UJS ID as the primary key
    """

    documents = []
    threshold = 5000

    def __init__(self):
        self.njs_client = NJSDatabaseClient()
        self.njs_jobs_collection = self.njs_client.get_jobs_collection()
        self.ujs_jobs_collection = UJSDatabaseClient().get_jobs_collection()

    @profile
    def insert_many(self):
        now = str(datetime.datetime.now())
        print(f"About to insert {len(self.documents)} records at {now}")
        self.njs_jobs_collection.insert_many(self.documents)
        self.documents = []

    @profile
    def insert_record(self, document):
        self.documents.append(document)
        if len(self.documents) >= self.threshold:
            self.insert_many()

    def go(self):
        ujs_cursor = self.ujs_jobs_collection.find()
        for job in ujs_cursor:
            ujs_id = str(job["_id"])
            new_entry = {"ujs_job_id": ujs_id}
            for key in job.keys():
                new_entry[key] = job[key]

            njs_job = self.njs_client.get_job(job_id=ujs_id)

            if njs_job is not None:

                njs_job_id = str(njs_job["_id"])

                del njs_job["_id"]
                del njs_job["ujs_job_id"]

                new_entry["njs_job_id"] = njs_job_id

                for key in njs_job.keys():
                    if key in new_entry.keys():
                        print("Error, found item in keys already: " + key)
                        print(ujs_id)
                        print(njs_job_id)
                        sys.exit("Error, found item in keys already:")

                    else:
                        new_entry[key] = njs_job[key]

            self.insert_record(new_entry)

        # Finish up
        self.insert_many()


if __name__ == "__main__":
    MigrateDatabases().go()
