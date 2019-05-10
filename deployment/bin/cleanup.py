#!/usr/bin/env python
import sys
import psutil
import docker
import logging
import os
import time

now = time.time()
logging.basicConfig(filename=f'cleanup_{now}.log', level=logging.INFO)

pid = sys.argv[1]
timeout = 86400 * 7
timeout = 10

logging.info(f"Waiting {timeout}s for {pid} to exit")

docker_job_ids_dir = "./docker_job_ids"
logs = os.listdir(docker_job_ids_dir)

container_ids = []
for item in logs:
  with open(docker_job_ids_dir + "/" + item, encoding='utf8') as f:
    container_ids.append(f.read().strip())

dc = docker.from_env()

for container_id in container_ids:
  try:
    logging.info(f"Attempting to kill {container_id}")
    c = dc.containers.get(container_id)
    #c.kill()
    #c.remove(v=True, force=True)
  except Exception as e:
    logging.error(e)

# TODO
# Log into ci04 and test this
