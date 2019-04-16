#!/usr/bin/env bash
NJSW_JAR=`readlink -f NJSWrapper-all.jar`
JOBID=$1
KBASE_ENDPOINT=$2

if [ -e /etc/clustername ] ; then
    # For NERSC let's check for local settings.  This could be allowed for all
    # locations
    #
    # This allows parameters to be modified for a specific location

    # HOME may not be set correctly so let's get it from the password file
    export HOME=$(getent passwd $(id -u)|cut -f6 -d:)
    if [ -e $HOME/.local_settings ] ; then
        . $HOME/.local_settings $1 $2
    fi
fi

#     export MINI_KB=true

BASE_DIR=$BASE_DIR/$JOBID
mkdir -p $BASE_DIR && cd $BASE_DIR
echo "Jar Location = $NJSW_JAR" > jar

trap "{ kill $pid }" SIGTERM

java -cp $NJSW_JAR $JAVA_OPTS us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr.out 2> sdk_lmr.err &
pid=$!
wait $pid
SDKLMR_EXITCODE=$?

java -cp $NJSW_JAR $JAVA_OPTS us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunnerCleanup $JOBID $KBASE_ENDPOINT > cleanup.out 2> cleanup.err
exit $SDKLMR_EXITCODE
