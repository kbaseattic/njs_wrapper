#!/usr/bin/env bash
JOBID=$1
TOKEN=$2
CLIENT_GROUPS=$3
KBASE_ENDPOINT=$4

if [ ! -z "$JOBID" ] && [ ! -z "$TOKEN" ] && [ ! -z "$CLIENT_GROUPS" ] && [ ! -z "$KBASE_ENDPOINT" ]; then

# *** DEVELOPMENT_MODE: CHANGE TO VOLUME MOUNTED DIRECTORY *** #
cd /njs_wrapper/
CLIENT_GROUPS=${CLIENT_GROUPS/ci/njs}
# *** DEVELOPMENT_MODE:                                    *** #
CLIENT_GROUPS=${CLIENT_GROUPS//\'//}

rm -f $JOBID
mkdir $JOBID
cd $JOBID
#cp /kb/deployment/misc/submit.sub .
rm -f submit.sub
echo "universe = vanilla"                           >> submit.sub
echo "accounting_group = sychan"                    >> submit.sub
echo "+Owner = \"condor_pool\""                     >> submit.sub
echo "executable = /kb/deployment/misc/submit.sh"   >> submit.sub

#transfer_input_files = distribution, random_words
echo "ShouldTransferFiles = YES"                    >> submit.sub
echo "when_to_transfer_output = ON_EXIT"            >> submit.sub

echo "request_cpus = 1"     >> submit.sub
echo "request_memory = 5MB" >> submit.sub
echo "request_disk = 1MB"   >> submit.sub

echo "log    = logfile.txt" >> submit.sub
echo "output = outfile.txt" >> submit.sub
echo "error  = errors.txt"  >> submit.sub
echo "getenv = true"        >> submit.sub


echo "requirements = $CLIENT_GROUPS " >> submit.sub




echo "environment = \"KB_AUTH_TOKEN=$TOKEN JOBID=$JOBID KBASE_ENDPOINT=$KBASE_ENDPOINT\"" >> submit.sub
echo "arguments = $JOBID" >> submit.sub
echo "batch_name = $JOBID" >> submit.sub
echo "queue 1" >> submit.sub

condor_submit -spool -terse submit.sub

else
>&2 echo "Not all variables are set in condor_submit.sh";
>&2 echo "JOBID=$JOBID";
>&2 echo "TOKEN=$TOKEN";
>&2 echo "CLIENT_GROUPS=$CLIENT_GROUPS";
>&2 echo "KBASE_ENDPOINT=$KBASE_ENDPOINT";
fi
