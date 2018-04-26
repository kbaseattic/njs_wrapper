#!/usr/bin/env bash
batchName=$1;
condorCommand="condor_rm  -constraint 'JobBatchName==\"$batchName\"'";
eval $condorCommand