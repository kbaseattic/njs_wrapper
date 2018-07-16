#!/usr/bin/env bash
if [ -e /etc/clustername ] ; then
    export PATH=$PATH:$(pwd)
    export USE_SHIFTER=1
    export REFDATA_DIR=/global/cscratch1/sd/kbaserun/refdata/ci/refdata/
    export CALLBACK_INTERFACE=ib0
    export SCRATCH=/global/homes/b/bsadkhin/freedom/scratch
    echo `pwd` > /global/homes/b/bsadkhin/freedom/pwd
    echo `ls` > /global/homes/b/bsadkhin/freedom/ls
    NJSW_JAR=`readlink -f NJSWrapper-all.jar`
    JOBID=$1
    KBASE_ENDPOINT=$2
    BASE_DIR=/global/homes/b/bsadkhin/jobs/$JOBID
    mkdir -p $BASE_DIR && cd $BASE_DIR
    echo "Jar Location = $NJSW_JAR" > jar
    env > env
    java -cp $NJSW_JAR us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr.out 2> sdk_lmr.err
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