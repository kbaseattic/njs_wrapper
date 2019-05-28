#!/usr/bin/env bash
mkdir -p ${JOB_DIR}/tmp
echo "Pre script has run" >  ${JOB_DIR}/pre.out
touch /mnt/awe/condor/bsadkhin/test/pre