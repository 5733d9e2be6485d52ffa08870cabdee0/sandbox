#!/bin/bash

########
# Run Fleet Shard locally in dev mode
#
# Env vars:
# - MANAGED_KAFKA_INSTANCE_NAME: set the managed kafka instance name (required)
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

. "${SCRIPT_DIR_PATH}/configure.sh" kafka minikube-started

mvn \
  -Dminikubeip=${MINIKUBE_IP} \
  -Dquarkus.http.port=1337 \
  -Pminikube \
  -f "${SCRIPT_DIR_PATH}/../../shard-operator/pom.xml" \
  clean compile quarkus:dev $@
