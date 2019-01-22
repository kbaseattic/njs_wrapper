#You will need to add this directory to the list of bind mounts in docker. 
#Add /mnt/condor in the "File Sharing tab"
mkdir -p /mnt/condor && chmod 777 /mnt/condor

docker-compose -f execution-engine.yml down
docker-compose -f execution-engine.yml pull
docker-compose -f execution-engine.yml up -d
docker-compose -f execution-engine.yml exec -u 0 njs bash

#You may need to run these manually on MacOSX by with docker exec
#docker-compose -f execution-engine.yml exec -u 0 condor_worker_mini "chmod 777 /run/docker.sock"
#docker-compose -f execution-engine.yml exec -u 0 condor_worker_max "chmod 777 /run/docker.sock"
