#!/usr/bin/env bash
if [ -e /etc/clustername ] ; then
  echo "NERSC"
  HOME=/global/homes/k/kbaserun
  $HOME/bin/run_async_srv_method.sh $@
else
#export MINI_KB=true
NJSW_JAR=`readlink -f NJSWrapper-all.jar`
JOBID=$1
KBASE_ENDPOINT=$2
BASE_DIR=$BASE_DIR/$JOBID
mkdir -p $BASE_DIR && cd $BASE_DIR
echo "Jar Location = $NJSW_JAR" > jar
java -cp $NJSW_JAR us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr.out 2> sdk_lmr.err
fi
