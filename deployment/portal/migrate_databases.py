#!/usr/bin/env python
import datetime
import sys
from memory_profiler import profile

from lib.njs_jobs import njs_jobs

"""

{'_id': ObjectId('5b843d9ee4b0d417818a2fb1'), 'user': 'jjeffryes', 'authstrat': 'DEFAULT',
 'authparam': 'DEFAULT', 'meta': [],
 'created': datetime.datetime(2018, 8, 27, 18, 6, 22, 902000),
 'updated': datetime.datetime(2018, 8, 27, 18, 7, 2, 129000), 'estcompl': None,
 'service': 'jjeffryes',
 'status': 'Job service side error: Output file is not found, exit code is 123[]',
 'desc': 'Execution engine job for TreeUtils.export_tree_newick', 'progtype': 'none',
 'started': datetime.datetime(2018, 8, 27, 18, 6, 47, 430000), 'complete': True, 'error': True,
 'errormsg': 'Fatal error: java.lang.IllegalStateException: Output file is not found, exit code is 123[]\n\tat us.kbase.narrativejobservice.sdkjobs.DockerRunner.run(DockerRunner.java:240)\n\tat us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner.main(SDKLocalMethodRunner.java:529)\n',
 'results': None, 'prog': 0, 'maxprog': None}
 
 
 {'_id': ObjectId('5b843da45428a14928929152'), 
 'ujs_job_id': '5b843d9ee4b0d417818a2fb1',
  'creation_time': 1535393188965, 
  'job_input': {'method': 'TreeUtils.export_tree_newick', 'requested_release': 'dev', 'params': [{'input_ref': '34844/17'}], 
  'service_ver': 'e3e807bb7b7c8fe503b4ad7a967202f28010ff2f'}, 
  'scheduler_type': 'condor', 
  'task_id': '1169.0 - 1169.0',
  'exec_start_time': 1535393207433, 
  'job_output': 
  {'error': {'code': -1, 'name': 
  'JSONRPCError', 'message': 'Job service side error: Output file is not found, exit code is 123[]', 'error': 'Fatal error: java.lang.IllegalStateException: Output file is not found, exit code is 123[]\n\tat us.kbase.narrativejobservice.sdkjobs.DockerRunner.run(DockerRunner.java:240)\n\tat us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner.main(SDKLocalMethodRunner.java:529)\n'}},
   'finish_time': 1535393222114}


:return: 
"""


class MigrateDatabases:
    documents = []
    threshold = 5000

    ujs_mongo_jobs = njs_jobs.get_ujs_database().get_collection(
        njs_jobs.ujs_jobs_collection
    )
    njs_mongo_jobs = njs_jobs.get_njs_database().get_collection(
        njs_jobs.njs_jobs_collection
    )

    @profile
    def insert_many(self):
        now = str(datetime.datetime.now())
        print(f"About to insert  {len(self.documents)} records at {now}")
        njs_mongo_jobs_collection = njs_jobs.get_njs_database().get_collection("jobs")
        njs_mongo_jobs_collection.insert_many(self.documents)
        self.documents = []

    @profile
    def insert_record(self, document):
        self.documents.append(document)
        if len(self.documents) > self.threshold:
            self.insert_many()

    def go(self):
        ujs_cursor = self.ujs_mongo_jobs.find()
        for job in ujs_cursor:
            ujs_id = str(job["_id"])
            new_entry = {"ujs_job_id": ujs_id}
            for key in job.keys():
                new_entry[key] = job[key]

            njs_job = self.njs_mongo_jobs.find_one({"ujs_job_id": ujs_id})

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
