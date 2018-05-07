#!/bin/bash
#
# Usage: run_monkey.sh [count]
#
if [ "$1" != "" ]; then
	count=$1
else
	count=100
fi

### Paramst
# Target device
TARGET=127.0.0.1:4444
# 1 if the target is AVD, 0 otherwise  
AVD=1
# Working paths
DIR="$( cd "$( dirname "$0" )" && pwd )"
LOGS=$DIR/../logs
EVENTS=$DIR/../events


### Main

runid="run-N${count}-`date '+%Y%m%d_%H%M'`"
echo "runid: $runid"

starttime=`date +'%s'`

adb -s ${TARGET} logcat -c
adb -s ${TARGET} logcat -G 14M

adb -s ${TARGET} logcat > ${LOGS}/logcat-${runid}.log &
logcatpid=$!
echo "logcat PID: ${logcatpid}"

if [ $AVD == 0 ]; then

	adb -s ${TARGET} shell monkey -vvv\
		 --ignore-crashes\
		 --ignore-timeouts\
		 --ignore-security-exceptions\
		 --ignore-native-crashes\
		 --monitor-native-crashes\
		 --pct-touch 9\
		 --pct-motion 9\
		 --pct-trackball 9\
		 --pct-syskeys 9\
		 --pct-nav 9\
		 --pct-majornav 9\
		 --pct-appswitch 9\
		 --pct-flip 10\
		 --pct-anyevent 9\
		 --pct-pinchzoom 9\
		 --pct-permission 9\
		 -s 100\
		 --throttle 500\
		 ${count}\
		 --script-log |& tee ${EVENTS}/events-monkey-${runid}.txt

else

	# With wearables devices (starting AW 2.0) we have to suppress 
	# the pct-syskeys.
	adb -s ${TARGET} shell monkey -vvv\
		 --ignore-crashes\
		 --ignore-timeouts\
		 --ignore-security-exceptions\
		 --ignore-native-crashes\
		 --monitor-native-crashes\
		 --pct-touch 10\
		 --pct-motion 10\
		 --pct-trackball 10\
		 --pct-syskeys 0\
		 --pct-nav 10\
		 --pct-majornav 10\
		 --pct-appswitch 10\
		 --pct-flip 10\
		 --pct-anyevent 10\
		 --pct-pinchzoom 10\
		 --pct-permission 10\
		 -s 100\
		 --throttle 500\
		 ${count}\
		 --script-log |& tee ${EVENTS}/events-monkey-${runid}.txt


fi


endtime=`date +'%s'`
totaltime=`expr $endtime - $starttime`

echo "Emulation completed in $totaltime sec."

sleep 10s
echo "Killing ${logcatpid}"
kill ${logcatpid}

#adb shell monkey -h
#
#usage: monkey [-p ALLOWED_PACKAGE [-p ALLOWED_PACKAGE] ...]
#              [-c MAIN_CATEGORY [-c MAIN_CATEGORY] ...]
#              [--ignore-crashes] [--ignore-timeouts]
#              [--ignore-security-exceptions]
#              [--monitor-native-crashes] [--ignore-native-crashes]
#              [--kill-process-after-error] [--hprof]
#              [--pct-touch PERCENT] [--pct-motion PERCENT]
#              [--pct-trackball PERCENT] [--pct-syskeys PERCENT]
#              [--pct-nav PERCENT] [--pct-majornav PERCENT]
#              [--pct-appswitch PERCENT] [--pct-flip PERCENT]
#              [--pct-anyevent PERCENT] [--pct-pinchzoom PERCENT]
#              [--pct-permission PERCENT]
#              [--pkg-blacklist-file PACKAGE_BLACKLIST_FILE]
#              [--pkg-whitelist-file PACKAGE_WHITELIST_FILE]
#              [--wait-dbg] [--dbg-no-events]
#              [--setup scriptfile] [-f scriptfile [-f scriptfile] ...]
#              [--port port]
#              [-s SEED] [-v [-v] ...]
#              [--throttle MILLISEC] [--randomize-throttle]
#              [--profile-wait MILLISEC]
#              [--device-sleep-time MILLISEC]
#              [--randomize-script]
#              [--script-log]
#              [--bugreport]
#              [--periodic-bugreport]
#              [--permission-target-system]
#              COUNT
#
