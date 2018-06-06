# Setup required files and directories
```
mkdir /njs_wrapper; chmod 777 /njs_wrapper
mkdir -p /mnt/awe/condor; chmod 777 /mnt/awe/condor
chmod 777 /var/run/docker.sock

cd /njs_wrapper/
git clone https://github.com/kbase/mini_kb.git
git clone https://github.com/kbase/jars.git
git clone https://github.com/kbase/njs_wrapper.git
git clone https://github.com/kbase/narrative.git
```
# Preparing mini-kb
Go to Docker->Preferences->FileSharing and add /njs_wrapper & /mnt/awe/condor


cd /njs_wrapper/mini_kb && edit the execution-engine.yml file to ensure that the njs, condor_worker_max and condor_worker_min images have the following volumes:
```
    volumes:
      - /mnt/awe/condor:/mnt/awe/condor      
      - /njs_wrapper/:/njs_wrapper/
      - /var/run/docker.sock:/run/docker.sock
```

## Launching mini-kb and getting inside
see [restart_minikb.sh](restart_minikb.sh)

# Redeploying your changes

## Manual Way
When you compile njs_wrapper, a jar and war file are created in `njs_wrapper/dist`. You would need to log into the njs container and swap out the 
`/kb/deployment/jettybase/webapps/root.war` file with your new `dist/NJSWrapper.war`
Then, you would need to log into the condor_workers and swap out the `/kb/deployment/lib/NJSWrapper.jar` with the new `NJSWrapper.jar`.

## Automated Way
Rebuild docker images with
`make docker_image`

Then you can edit the execution-engine.yml and add your new image tag there
```
njs:
  image: YourNewImageName

condor_worker_max:
  image: YourNewImageName
  
condor_worker_min:
  image: YourNewImageName
```


# Testing in mini_kb

* Copy over the dev_tools/deploy_local_minikb.cfg to /njs_wrapper/njs_wrapper/deploy.cfg to do local testing
* Ensure the test.cfg points to the same `self.external.url` as in the deploy.cfg
* Ensure the token is populated to the minikb token
* Ensure the `/kb/deployment/misc/sdklocalmethodrunner.sh` script has `MINI_KB=true` and has the correct KB_ADMIN_AUTH_TOKEN for ci
* Ensure that the src/us/kbase/narrativejobservice/sdkjobs/DockerRunner.java runner has the correct network with

```
cntCmd.withNetworkMode("mini_kb");
```
## Log into the njs container
 * Install test dependencies with dev_tools/install.sh
 * Ensure you are the kbase user 
 * Run a test such as `cd /njs_wrapper/njs_wrapper/ && ant testCondorintegration`
### Test from your local machine
Add `127.0.0.01 nginx` to your hosts file

  
