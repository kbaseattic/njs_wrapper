#!/bin/sh
export JAVA_HOME=AVA_HOME
export PATH=AVA_HOME/bin:$PATH
java -cp "/kb/deployment/lib/*" us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $1 $2 $3 $4 >job_out.txt 2>job_err.txt

