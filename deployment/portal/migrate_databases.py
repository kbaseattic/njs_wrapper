#!/usr/bin/env python
from .lib.njs_jobs import njs_jobs


ujs_mongo_jobs = njs_jobs.get_ujs_database().get_collection(njs_jobs.ujs_jobs_collection)
njs_mongo_jobs = njs_jobs.get_njs_database().get_collection(njs_jobs.njs_jobs_collection)


ujs_cursor = ujs_mongo_jobs.find()
for job in ujs_cursor():
    print(job)