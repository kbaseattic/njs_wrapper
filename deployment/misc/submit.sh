#!/usr/bin/env bash
export KBASE_ENDPOINT=http://nginx/services
export KB_AUTH_TOKEN=62IYPZGS7O773DBLZZCSE542BP4C2E7G
export KB_DOCKER_NETWORK=minikb_default


jobID=$1
#java -cp "/kb/deployment/lib/*" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $jobID
cd /njs_wrapper/njs_wrapper/$jobID
java -cp "/njs_wrapper/lib/*" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $jobID $KBASE_ENDPOINT/njs