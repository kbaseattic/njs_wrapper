njs=`docker ps | grep mini_kb_njs_1 | cut -f1 -d' '`
max=`docker ps | grep mini_kb_condor_worker_max | cut -f1 -d' '`
min=`docker ps | grep mini_kb_condor_worker_mini | cut -f1 -d' '`

docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.jar $njs:/kb/deployment/lib/
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.jar $max:/kb/deployment/lib/
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.jar $min:/kb/deployment/lib/
docker cp /njs_wrapper/njs_wrapper/dist/NJSWrapper.war $njs:/kb/deployment/jettybase/webapps/root.war
