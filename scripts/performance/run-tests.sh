#!/bin/bash

if [ ! -f /tmp/qDup.jar  ]; then
  wget --output-document=/tmp/qDup.jar https://github.com/Hyperfoil/qDup/releases/download/qDup-0.6.14/qDup-0.6.14-uber.jar
  status=$?
  if [ $status -ne 0 ]; then
    echo "Failed to download qDup, exitcode: $status"
    exit 1
  fi
fi

java -jar /tmp/qDup.jar qdup.yaml secrets.yaml

