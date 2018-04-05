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
