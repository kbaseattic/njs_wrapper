#!/usr/bin/env bash

njs=`docker ps | grep mini_kb_njs_1 | cut -f1 -d' '`
max=`docker ps | grep mini_kb_condor_worker_max | cut -f1 -d' '`
min=`docker ps | grep mini_kb_condor_worker_mini | cut -f1 -d' '`

docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.jar $njs:/kb/deployment/lib/
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.jar $max:/kb/deployment/lib/
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.jar $min:/kb/deployment/lib/
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.war $njs:/kb/deployment/jettybase/webapps/root.war

#Optional copy over other dependencies or new sdk_localmethodrunner script
#docker cp /njs_wrapper/njs_wrapper/deployment/misc/sdklocalmethodrunner.sh $njs:/kb/deployment/misc/
#docker cp /njs_wrapper/njs_wrapper/deployment/misc/docker3dependencies $njs:/kb/deployment/misc/
#docker cp /njs_wrapper/njs_wrapper/deployment/misc/sdklocalmethodrunner.sh $max:/kb/deployment/misc/
#docker cp /njs_wrapper/njs_wrapper/deployment/misc/docker3dependencies $max:/kb/deployment/misc/
#docker cp /njs_wrapper/njs_wrapper/deployment/misc/sdklocalmethodrunner.sh $min:/kb/deployment/misc/
#docker cp /njs_wrapper/njs_wrapper/deployment/misc/docker3dependencies $min:/kb/deployment/misc/

echo "Done copying jars/wars/(maybe other things) to /kb/deployment/lib for njs,condor_max,condor_min"


