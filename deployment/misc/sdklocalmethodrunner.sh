#!/usr/bin/env bash
JOBID=$1
KBASE_ENDPOINT=$2
BASE_DIR=$BASE_DIR/$JOBID
mkdir -p $BASE_DIR && cd $BASE_DIR
java -cp "/kb/deployment/lib/*" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr_logs.txt