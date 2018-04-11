#!/usr/local/env bash

PROGRAM=$1
JOB_ID=$2
JOB_SERVICE_URL=$3
STDERR=$JOB_ID.error
STDOUT=$JOB_ID.out

java -cp "/kb/deployment/lib/*" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $PROGRAM $JOB_ID $JOB_SERVICE_URL >$STDOUT 2>$STDERR