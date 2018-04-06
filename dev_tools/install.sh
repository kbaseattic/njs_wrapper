#!/usr/bin/env bash
#RUN AS ROOT
#Install dependences in order to run the build tar and do development run of
# ant condorTest | ant testCondorIntegration
apt-get update
apt-get -y install git;
apt-get -y install ant;
apt-get -y install default-jdk;
apt-get -y install mongodb;
apt-get -y install procps;
apt-get -y install htop;
apt-get -y install vim;
apt-get -y install nmap;
apt-get -y install gdb;
apt-get -y install make;
apt-get -y install docker;

#Somewhere in here docker gets installed
apt-get install apt-transport-https ca-certificates software-properties-common
curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - ;
add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian strech stable";
apt-get install docker-ce;
wget https://download.docker.com/linux/debian/dists/stretch/pool/stable/amd64/docker-ce_18.03.0~ce-0~debian_amd64.deb
dpkg -i docker-ce_18.03.0~ce-0~debian_amd64.deb
apt -y --fix-broken install

#Enable testing of local jar upon compile
rm /kb/deployment/lib/NJSWrapper.jar
ln -s /njs/dist/NJSWrapper.jar /kb/deployment/lib/NJSWrapper.jar
