#!/bin/bash
# This script requires to have Go language ver.1.5.4 installed
script_dir="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
GO_TMP_DIR=$script_dir/gobuild
rm -rf $GO_TMP_DIR
git clone -b auth2 https://github.com/kbase/AWE $GO_TMP_DIR/src/github.com/MG-RAST/AWE
mkdir -p $GO_TMP_DIR/src/github.com/docker
wget -O $GO_TMP_DIR/src/github.com/docker/docker.zip https://github.com/docker/docker/archive/v1.6.1.zip
unzip -d $GO_TMP_DIR/src/github.com/docker $GO_TMP_DIR/src/github.com/docker/docker.zip
mv -v $GO_TMP_DIR/src/github.com/docker/docker-1.6.1 $GO_TMP_DIR/src/github.com/docker/docker
export GOPATH=$GO_TMP_DIR; go get -v github.com/MG-RAST/AWE/...
cp -v $GO_TMP_DIR/bin/awe-server ./awe-server
cp -v $GO_TMP_DIR/bin/awe-client ./awe-client
rm -rf $GO_TMP_DIR