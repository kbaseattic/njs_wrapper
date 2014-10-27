#!/bin/bash

if (( $# != 1 ))
then
    echo "Usage: glassfish_stop_service <target_port>"
    exit
fi

TARGET_PORT=$1
DOMAIN=domain1
DOMAINDIR=$(dirname $0)/glassfish

if [ -z "$KB_RUNTIME" ]
then
   export KB_RUNTIME=/kb/runtime
fi

if [ -z "$GLASSFISH_HOME" ]
then
   export GLASSFISH_HOME=$KB_RUNTIME/glassfish3
fi

asadmin=$GLASSFISH_HOME/glassfish/bin/asadmin

$asadmin list-domains --domaindir $DOMAINDIR | grep '$DOMAIN running' > /dev/null
if [ $? -eq 0 ]; then
    echo "Glassfish is already running."
else
    $asadmin start-domain --domaindir $DOMAINDIR $DOMAIN
fi
$asadmin list-applications | grep app-${TARGET_PORT} > /dev/null
if [ $? -eq 0 ]; then
    $asadmin undeploy app-${TARGET_PORT}
fi

$asadmin list-http-listeners | grep http-listener-${TARGET_PORT} > /dev/null
if [ $? -eq 0 ]; then
    $asadmin delete-http-listener http-listener-${TARGET_PORT}
fi

$asadmin list-protocols | grep http-listener-${TARGET_PORT} > /dev/null
if [ $? -eq 0 ]; then
    $asadmin delete-protocol http-listener-${TARGET_PORT}
fi

$asadmin list-threadpools server | grep thread-pool-${TARGET_PORT} > /dev/null
if [ $? -eq 0 ]; then
    $asadmin delete-threadpool thread-pool-${TARGET_PORT}
fi

$asadmin list-virtual-servers | grep server-${TARGET_PORT} > /dev/null
if [ $? -eq 0 ]; then
    $asadmin delete-virtual-server server-${TARGET_PORT}
fi

$asadmin list-applications | grep app-${TARGET_PORT}

if [ $($asadmin list-applications | grep app-${TARGET_PORT}|wc -l) -eq 0 ] ; then
  PID="$DOMAINDIR/$DOMAIN/config/pid"
  echo "No more Apps. Killing glassfish server"
  if [ -e $PID ] ; then
    kill $(cat $PID)
  fi
fi