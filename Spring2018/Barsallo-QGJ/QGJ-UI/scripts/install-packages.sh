#!/bin/bash
LOG="app_install.log"

rm -f ${LOG}
for app in `ls ../apks/wear/popular_apps/*.apk`; do
	echo $app
	echo $app >> ${LOG}
	adb -s emulator-5554 install -r $app >> ${LOG} 2>&1
done
