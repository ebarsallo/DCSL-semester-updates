#!/bin/bash
#
# Usage: run_commands.sh [semi|rand|orig]
#

if [ "$1" != "" ]; then
	count=$1
else
	count=100
fi

### Paramst
# Target device
TARGET=127.0.0.1:4444
# Working paths
DIR="$( cd "$( dirname "$0" )" && pwd )"
LOGS=$DIR/../logs
CMDS=$DIR/../commands/

f="${CMDS}ape_commands_orig.sh"
if [ "$1" = "orig" ]; then
	f="${CMDS}ape_commands_orig.sh"
elif [ "$1" = "semi" ]; then
	f="${CMDS}ape_commands_semivalid.sh"
elif [ "$1" = "rand" ]; then
	f="${CMDS}ape_commands_random.sh"
fi

### Functions

function run_cmd(){
	adb -s ${TARGET} ${@}
}

### Main

runid="run-N${count}-`date '+%Y%m%d_%H%M'`"
echo "runid: $runid"

starttime=`date +'%s'`

adb -s ${TARGET} logcat -c
adb -s ${TARGET} logcat -G 14M
adb -s ${TARGET} logcat VVV > logcat-${runid}.log &
logcatpid=$!
echo "logcat PID: ${logcatpid}"
adb -s ${TARGET} shell log -t "DSN18" "*** Starting Ex.: ${runid} ***"


# Start from home screen
adb -s ${TARGET} shell am start -a android.intent.action.MAIN -c android.intent.category.HOME

while IFS='' read -ru 3 line; do
	echo "$line"
	cmd=$(echo $line | sed 's/^[0-9]\+\://g')
	echo "adb -s emulator-5554 $cmd"
	run_cmd $cmd
	sleep 0.5s
done 3<${f}

# Finish with home screen
adb -s ${TARGET} shell am start -a android.intent.action.MAIN -c android.intent.category.HOME

adb -s ${TARGET} shell log -t "DSN18" "*** Finishing Ex.: ${runid} ***"

endtime=`date +'%s'`
totaltime=`expr $endtime - $starttime`

echo "Emulation completed in $totaltime sec."

sleep 60s
echo "Killing ${logcatpid}"
kill ${logcatpid}

