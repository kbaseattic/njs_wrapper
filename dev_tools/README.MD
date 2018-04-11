# Welcome to the readme

* Step 1: Choose your test environment, CI or the local minikb install, and copy over the dev_tools/deploy.cfg to the main directory as "deploy.cfg"

* Step 2: Edit your test.cfg file with the correct token

* Step 3: Edit the dockerfile in njs_wrapper if you want it to come with the pre-reqs installed in install.sh. Then update the execution-engine.yml image for njs to point to that image. Don't forget to mount this directory inside njs_image as well, so you have access to the tests

* Step 4: Run the ant directive `ant testCondorIntegration`

* Step 5: To make changes and have them immediately available for testing, do `make redeploy`