
echo "Condor Utils::condor_q.sh:: Calling condor_q with param JobId $1 running on `whoami`@`hostname`"

condor_q $1
