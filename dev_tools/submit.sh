export KBASE_ENDPOINT=http://nginx/services
export KB_AUTH_TOKEN=62IYPZGS7O773DBLZZCSE542BP4C2E7G
export KB_DOCKER_NETWORK=minikb_default

#export KB_ADMIN_AUTH_TOKEN=62IYPZGS7O773DBLZZCSE542BP4C2E7G
jobID=$1
java -cp "/kb/deployment/lib/*" -DKB_AUTH_TOKEN="62IYPZGS7O773DBLZZCSE542BP4C2E7G" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $jobID http://nginx/services/njs
