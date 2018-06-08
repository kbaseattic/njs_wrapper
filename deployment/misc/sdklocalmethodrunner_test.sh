#!/usr/bin/env bash


export JOBID=$1
export KBASE_ENDPOINT=$2
export BASE_DIR=$BASE_DIR/$JOBID
export KB_ADMIN_AUTH_TOKEN=62IYPZGS7O773DBLZZCSE542BP4C2E7G
export KB_AUTH_TOKEN=62IYPZGS7O773DBLZZCSE542BP4C2E7G
mkdir -p $BASE_DIR && cd $BASE_DIR

cp=/kb/deployment/lib
declare -a deps
readarray -t deps < /kb/deployment/misc/requirements.txt
dependencies_list=$(printf ":$cp/%s" "${deps[@]}")
dependencies_list=${dependencies_list:1}


#DEV
echo java -cp "$dependencies_list" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > command
env > env
MINI_KB=true

java -cp "$dependencies_list" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr.out 2> sdk_lmr.err

java -cp java -cp "/kb/deployment/lib/NJSWrapper.jar:/kb/deployment/lib/WorkspaceClient-0.6.0.jar:/kb/deployment/lib/aopalliance-repackaged-2.3.0-b05.jar:/kb/deployment/lib/bcpkix-jdk15on-1.51.jar:/kb/deployment/lib/bcprov-jdk15on-1.51.jar:/kb/deployment/lib/bson4jackson-2.2.0-2.2.0.jar:/kb/deployment/lib/cglib-nodep-2.2.jar:/kb/deployment/lib/commons-codec-1.8.jar:/kb/deployment/lib/commons-compress-1.5.jar:/kb/deployment/lib/commons-io-2.4.jar:/kb/deployment/lib/commons-lang-2.4.jar:/kb/deployment/lib/commons-lang3-3.1.jar:/kb/deployment/lib/commons-logging-1.1.1.jar:/kb/deployment/lib/core-matchers-1.6.jar:/kb/deployment/lib/derby-10.10.1.1.jar:/kb/deployment/lib/docker-java-3.0.14.jar:/kb/deployment/lib/easymock-3.2.jar:/kb/deployment/lib/guava-18.0.jar:/kb/deployment/lib/hamcrest-core-1.3.jar:/kb/deployment/lib/hamcrest-library-1.3.jar:/kb/deployment/lib/hk2-api-2.3.0-b05.jar:/kb/deployment/lib/hk2-locator-2.3.0-b05.jar:/kb/deployment/lib/hk2-utils-2.3.0-b05.jar:/kb/deployment/lib/httpclient-4.3.1.jar:/kb/deployment/lib/httpcore-4.3.jar:/kb/deployment/lib/httpmime-4.3.1.jar:/kb/deployment/lib/ini4j-0.5.2.jar:/kb/deployment/lib/jackson-annotations-2.6.4.jar:/kb/deployment/lib/jackson-core-2.6.4.jar:/kb/deployment/lib/jackson-databind-2.6.4.jar:/kb/deployment/lib/jackson-jaxrs-base-2.6.4.jar:/kb/deployment/lib/jackson-jaxrs-json-provider-2.6.4.jar:/kb/deployment/lib/jackson-module-jaxb-annotations-2.6.4.jar:/kb/deployment/lib/javax.annotation-api-1.2.jar:/kb/deployment/lib/javax.inject-2.3.0-b05.jar:/kb/deployment/lib/javax.ws.rs-api-2.0.jar:/kb/deployment/lib/jersey-apache-connector-2.11.jar:/kb/deployment/lib/jersey-client-2.11.jar:/kb/deployment/lib/jersey-common-2.11.jar:/kb/deployment/lib/jersey-guava-2.11.jar:/kb/deployment/lib/jetty-all-7.0.0.jar:/kb/deployment/lib/jna-3.4.0.jar:/kb/deployment/lib/joda-time-2.2.jar:/kb/deployment/lib/jongo-0.5-early-20130912-1506.jar:/kb/deployment/lib/jpa-matchers-1.6.jar:/kb/deployment/lib/junit-4.9.jar:/kb/deployment/lib/junixsocket-common-2.0.4.jar:/kb/deployment/lib/junixsocket-native-common-2.0.4.jar:/kb/deployment/lib/kbase-auth-0.4.4.jar:/kb/deployment/lib/kbase-common-0.0.23.jar:/kb/deployment/lib/lambdaj-2.3.3.jar:/kb/deployment/lib/logback-classic-1.1.2.jar:/kb/deployment/lib/logback-core-1.1.2.jar:/kb/deployment/lib/mongo-java-driver-2.13.3.jar:/kb/deployment/lib/native-lib-loader-2.2.0.jar:/kb/deployment/lib/objenesis-1.2.jar:/kb/deployment/lib/osgi-resource-locator-1.0.1.jar:/kb/deployment/lib/persistence-api-1.0.jar:/kb/deployment/lib/scijava-common-2.69.0.jar:/kb/deployment/lib/servlet-api-2.5.jar:/kb/deployment/lib/shock-client-0.0.15.jar:/kb/deployment/lib/slf4j-api-1.7.7.jar:/kb/deployment/lib/syslog4j-0.9.46.jar" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT