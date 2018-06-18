#!/usr/bin/env bash
JOBID=$1
KBASE_ENDPOINT=$2
BASE_DIR=$BASE_DIR/$JOBID
export MINI_KB=true
mkdir -p $BASE_DIR && cd $BASE_DIR
java -cp "/kb/deployment/lib/dist/NJSWrapper-all.jar" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr.out 2> sdk_lmr.err
