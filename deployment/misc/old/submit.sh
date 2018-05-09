#!/usr/bin/env bash
export KB_DOCKER_NETWORK=minikb_default
if [ ! -z "$JOBID" ] && [ ! -z "$KB_AUTH_TOKEN" ] && [ ! -z "$CLIENT_GROUPS" ] && [ ! -z "$KBASE_ENDPOINT" ]; then

# *** DEVELOPMENT_MODE: CHANGE TO VOLUME MOUNTED DIRECTORY *** #
cd /njs_wrapper/$JOBID


>&2 echo "All variables properly set";
echo $KB_AUTH_TOKEN > token
java -cp "/kb/deployment/lib/*" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT

else
>&2 echo "Not all variables are set in submit.sh";
>&2 echo "JOBID=$JOBID";
>&2 echo "KB_AUTH_TOKEN=$KB_AUTH_TOKEN";
>&2 echo "CLIENT_GROUPS=$CLIENT_GROUPS";
>&2 echo "KBASE_ENDPOINT=$KBASE_ENDPOINT";
fi
