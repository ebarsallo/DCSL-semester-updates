#!/bin/bash
adb -s emulator-5554 logcat -G 16M

for i in `seq 1 1000`; do

starttime=`date +'%Y%m%d_%H%M%S'`

echo "Starting logcat at: `date` file: logcat-${starttime}.log"

adb -s emulator-5554 logcat VVV > logcat-${starttime}.log 
logcatexit=$?
##echo "logcat PID: ${logcatpid}"
##adb -s emulator-5554 shell log -t "DSN18" "*** Starting Ex.: ${runid} ***"
echo "Logcat exited at: `date`"

if [ ${logcatexit} -ne 0 ]; then
	echo "Non-zero exit status from logcat. Waiting 2s"
	sleep 5s
fi

done

