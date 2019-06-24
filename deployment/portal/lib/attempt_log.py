from lib.NJSDatabaseClient import NJSDatabaseClient

ndc = NJSDatabaseClient()
ujs_job_id = "565f5b6ee4b0a6527cead4a2"
job = ndc.get_log(ujs_job_id)
print(job)
