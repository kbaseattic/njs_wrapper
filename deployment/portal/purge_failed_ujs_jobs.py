#!/usr/bin/env python

from lib.ExecutionEngineJobs import ExecutionEngineJobs

eej = ExecutionEngineJobs()
eej.purge_incomplete_jobs(dry_run=True)
