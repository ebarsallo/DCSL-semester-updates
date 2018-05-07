#!/bin/bash

for f in `cat files.txt`
do
   echo $f
   diff $f /tmp/Squibble-before-dsn/$f
done
