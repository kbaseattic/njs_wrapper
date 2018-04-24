#!/usr/bin/env bash

#Enable testing of local jar upon compile
#cd /njs_wrapper/njs_wrapper;
#make redeploy;

#rm /njs_wrapper/lib/NJSWrapper.jar
#ln -s /njs_wrapper/njs_wrapper/dist/NJSWrapper.jar /njs_wrapper/lib/NJSWrapper.jar
#rm /kb/deployment/lib/NJSWrapper.jar
#ln -s /njs_wrapper/njs_wrapper/dist/NJSWrapper.jar /kb/deployment/lib/NJSWrapper.jar

cp /njs_wrapper/njs_wrapper/deployment/misc/* /kb/deployment/misc/

