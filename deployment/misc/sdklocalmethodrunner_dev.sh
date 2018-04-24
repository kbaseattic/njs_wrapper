#!/usr/bin/env bash
JOBID=$1
KBASE_ENDPOINT=$2
mkdir /njs_wrapper/$JOBID && cd /njs_wrapper/$JOBID
java -cp "/kb/deployment/lib/*" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT