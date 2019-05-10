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

#Set up job and tmp directories
BASE_DIR=$BASE_DIR/$JOBID
TMP_DIR=$BASE_DIR/tmp
mkdir -p $BASE_DIR && cd $BASE_DIR
mkdir -p $TMP_DIR

#Set up java options
JAVA_OPTS="-Djava.io.tmpdir=$TMP_DIR "
ulimit -c unlimited


date="date +'%s'"
#Move Jar to work directory
echo "Jar Location = $NJSW_JAR" > jar_$date
env > env_$date

mv $NJSW_JAR njsw.jar

#Trap condor_rm
trap "{ kill $pid }" SIGTERM

#Run the job runner and then clean up after it's done

java $JAVA_OPTS -cp njsw.jar  us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr_$date.out 2> sdk_lmr_$date.err &
pid=$!

python cleanup.py $pid &
wait $pid
SDKLMR_EXITCODE=$?

touch endsdklmr
java $JAVA_OPTS -cp njsw.jar  us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunnerCleanup $JOBID $KBASE_ENDPOINT > cleanup_$date.out 2> cleanup_$date.err
touch endcleanjob
exit $SDKLMR_EXITCODE
