# Relevant Links
* https://github.com/kbase/njs_wrapper
* https://github.com/kbase/mini_kb

# Old Documentation
* https://github.com/kbase/mini_kb/blob/master/README-execution-engine.md 
* https://github.com/kbase/njs_wrapper/blob/develop/dev_tools/TestingNJS.md

# Setup & Known issues
* On Mac, you need to log into the workers (docker ps | grep worker and  docker exec -it -u 0 <CNTID> bash)
and `chmod 777 /var/run/docker.sock` and `/run/docker.sock` 
* Add “127.0.0.1 nginx” “127.0.0.1 ci-mongo” to your /etc/hosts file
* `mkdir -p /mnt/condor` on your host machine to see the job output files

# How to run the integration tests with mini_kb
* copy /test.cfg.example to test.cfg and add either the mini_kb token from [here](https://github.com/kbase/mini_kb/blob/master/deployment/conf/njs-wrapper-minikb.ini#L18)
* copy /dev_tools/deploy_local_minikb.cfg to /deploy.cfg
	`gradle test --tests CondorIntegrationTest`

# How to run the integration test against ci/appdev/prod
* copy /test.cfg.example to test.cfg and add your token
* copy /dev_tools/deploy_local_minikb.cfg to /deploy.cfg and edit variables for the correct production environment
	`gradle test --tests CondorIntegrationTest`

# How to build
`gradle buildall`

# How to add more jars?
Add them via the kbase artifactory and/or edit the gradle.build file to pull them from a public repo
