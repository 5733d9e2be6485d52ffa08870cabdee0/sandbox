#!/bin/bash

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

usage() {
    echo 'Usage: run-local-tests.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -t $TAGS            Tags to use for selecting scenario'
    echo '  -p                  To be set if you want to run the tests in parallel mode'
    echo '  -P                  To run performance tests'
    echo '  -k                  Keep created data, aka do not perform cleanup. Mostly used for local test run and debug.'
    echo
    echo 'Examples:'
    echo '  # Simple run'
    echo '  sh run-local-tests.sh'
    echo
    echo '  # Run scenarios in parallel with `@test` tag'
    echo '  sh run-local-tests.sh -p -t test'
}

ARGS=
MAVEN_PROFILE="cucumber"

while getopts "t:pPkh" i
do
    case "$i"
    in
        p) ARGS="${ARGS} -Dparallel" ;;
        P) MAVEN_PROFILE="performance" ;;
        t) ARGS="${ARGS} -Dgroups=${OPTARG}" ;;
        k) ARGS="${ARGS} -Dcleanup.disable" ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

BIN_DIR=${SCRIPT_DIR_PATH}/../../../dev/bin
INTEGRATION_TESTS_DIR=${SCRIPT_DIR_PATH}

. ${BIN_DIR}/configure.sh minikube-started
. ${LOCAL_ENV_FILE}

cd ${INTEGRATION_TESTS_DIR}

set -x # activate printing executed commands

mvn clean verify $ARGS \
  -P$MAVEN_PROFILE \
  -Devent-bridge.manager.url=${MANAGER_URL} \
  -Dkeycloak.realm.url=${KEYCLOAK_URL}/auth/realms/event-bridge-fm \
  -Dtest.credentials.file=localconfig.yaml