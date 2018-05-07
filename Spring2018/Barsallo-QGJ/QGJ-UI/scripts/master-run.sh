#!/bin/bash

# Working paths
DIR="$( cd "$( dirname "$0" )" && pwd )"
LOGS=$DIR/../logs

# suffix=`date +'%Y%m%d_%H%M'`
# ./run_commands.sh "rand" |& tee ex_random_commands_${suffix}.log

suffix=`date +'%Y%m%d_%H%M'`
${DIR}/run_commands.sh "semi" |& tee ${LOGS}/ex_semivalid_commands_${suffix}.log
