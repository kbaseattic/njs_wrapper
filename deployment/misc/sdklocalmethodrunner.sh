#!/usr/bin/env bash
JOBID=$1
KBASE_ENDPOINT=$2
BASE_DIR=/mnt/awe/condor/$JOBID
cd $BASE_DIR
java -cp "/kb/deployment/lib/*" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT