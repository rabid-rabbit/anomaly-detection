#!/bin/sh

function help {
  echo "local|remote <number_of_days> <number_of_neighbours>"
}

if [ "$#" -ne 3 ]; then
 echo "Incorrect number of input arguments"
 help
 exit 1
fi

if [ "x$1" == "xlocal" ]; then
 sbt -Dspark.master=local "run $2 $3"
fi
