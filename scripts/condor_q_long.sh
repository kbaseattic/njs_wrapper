
echo "Condor Utils::condor_q_long.sh:: Calling condor_q with param JobId $1, for attribute $2; running on `whoami`@`hostname`"

condor_q $1 -long | grep $2
