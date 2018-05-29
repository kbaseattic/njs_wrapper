#!/usr/bin/env bash
batchName=$1;
attributes=$2;
condorCommand="condor_q  -constraint 'JobBatchName==\"$batchName\"' -attributes $attributes -json";
eval $condorCommand