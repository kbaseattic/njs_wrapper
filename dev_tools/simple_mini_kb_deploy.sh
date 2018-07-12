#!/usr/bin/env bash
njs=`docker ps | grep mini_kb_njs_1 | cut -f1 -d' '`
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper-all.jar $njs:/kb/deployment/lib/
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.war $njs:/kb/deployment/jettybase/webapps/root.war
docker cp /njs_wrapper/njs_wrapper/deployment/misc/sdklocalmethodrunner.sh $njs:/kb/deployment/misc/

#max=`docker ps | grep mini_kb_condor_worker_max | cut -f1 -d' ' | head -n1`
#min=`docker ps | grep mini_kb_condor_worker_mini | cut -f1 -d' ' | head -n1`


#docker cp /njs_wrapper/njs_wrapper/dist/ $max:/kb/deployment/lib/
#docker cp /njs_wrapper/njs_wrapper/dist/ $min:/kb/deployment/lib/


#docker cp /njs_wrapper/njs_wrapper/deployment/misc/docker3dependencies $njs:/kb/deployment/misc/

#docker cp /njs_wrapper/njs_wrapper/deployment/misc/sdklocalmethodrunner.sh $max:/kb/deployment/misc/
#docker cp /njs_wrapper/njs_wrapper/deployment/misc/docker3dependencies $max:/kb/deployment/misc/

#docker cp /njs_wrapper/njs_wrapper/deployment/misc/sdklocalmethodrunner.sh $min:/kb/deployment/misc/
#docker cp /njs_wrapper/njs_wrapper/deployment/misc/docker3dependencies $min:/kb/deployment/misc/


Echo "Done"


