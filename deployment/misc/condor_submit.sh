#!/usr/bin/env bash

export baseDir="/njs_wrapper/njs_wrapper/$1"
mkdir $baseDir
cd $baseDir

cp /kb/deployment/misc/submit.sub .

echo "initialdir = $baseDir" >> submit.sub
echo "arguments = $1" >> submit.sub
echo "batch_name = $1" >> submit.sub
echo "queue 1" >> submit.sub

command="condor_submit -spool -terse submit.sub"
echo $command > "condor_submit.bash"

$command