#!/bin/bash

########
# Run Fleet Shard locally in dev mode
#
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

export MANAGED_KAFKA_INSTANCE_NAME=rhose-local-development

. "${SCRIPT_DIR_PATH}/configure.sh" kafka minikube-started

mvn \
  -Dminikubeip=${MINIKUBE_IP} \
  -Dquarkus.http.port=1337 \
  -Devent-bridge.logging.json=false \
  -Devent-bridge.k8s.orchestrator=minikube \
  -f "${SCRIPT_DIR_PATH}/../../shard-operator/pom.xml" \
  clean compile quarkus:dev $@
