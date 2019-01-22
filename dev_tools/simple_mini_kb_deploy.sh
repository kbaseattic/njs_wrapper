#!/usr/bin/env bash
njs=`docker ps | grep mini_kb_njs_1 | cut -f1 -d' '`
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper-all.jar $njs:/kb/deployment/lib/
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.war $njs:/kb/deployment/jettybase/webapps/root.war
docker cp /njs_wrapper/njs_wrapper/deployment/misc/sdklocalmethodrunner_dev.sh $njs:/kb/deployment/misc/sdklocalmethodrunner.sh
Echo "Done copying NJSWrapper-all.jar  NjsWrapper.war sdklocalmethodrunner_dev.sh into mini_kb"


