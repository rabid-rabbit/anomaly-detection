#!/bin/sh

function help {
  echo "local <command> <config>|remote <spark-master-url> <command> <config>"
}

BASEDIR=$(dirname $0)
EXECUTOR_MEMORY=10G
DRIVER_MEMORY=2G

if [ "$#" -lt 3 ]; then
  echo "Incorrect number of input arguments"
  help
  exit 1
fi

if [ "x$1" == "xlocal" ]; then

 (cd $BASEDIR; sbt -Dspark.master=local "run $2 $3")

fi

if [ "x$1" == "xremote" ]; then

 if [ "$#" -ne 4 ]; then
  echo "Incorrect number of input arguments"
  help
  exit 1
 fi

 if ! command -v spark-submit > /dev/null; then
  echo "Could not find spark-submit script. Please make sure its in the PATH."
  exit 2
 fi

 if [ ! -f $BASEDIR/target/scala-2.11/anomaly-detection-assembly-1.0.jar ]; then
    sbt clean assembly
 fi

 spark-submit --class com.sungevity.analytics.Main --executor-memory $EXECUTOR_MEMORY --driver-memory $DRIVER_MEMORY --master $2 $BASEDIR/target/scala-2.11/anomaly-detection-assembly-1.0.jar $3 $4

fi


