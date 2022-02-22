#!/bin/bash

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

BIN_DIR=${SCRIPT_DIR_PATH}/../dev/bin
INTEGRATION_TESTS_DIR=${SCRIPT_DIR_PATH}

. ${BIN_DIR}/configure.sh minikube-started
. ${LOCAL_ENV_FILE}

cd ${INTEGRATION_TESTS_DIR}

mvn clean verify \
  -Pcucumber \
  -Devent-bridge.manager.url=${MANAGER_URL} \
  -Dkeycloak.realm.url=${KEYCLOAK_URL}/auth/realms/event-bridge-fm
  # -Dbridge.token.username=${} \
  # -Dbridge.token.password=${}