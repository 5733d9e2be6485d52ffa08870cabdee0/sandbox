#!/bin/bash

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

usage() {
    echo 'Usage: run-local-tests.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -t $TAGS            Tags to use for selecting scenario'
    echo '  -p                  To be set if you want to run the tests in parallel mode'
    echo
    echo 'Examples:'
    echo '  # Simple run'
    echo '  sh run-local-tests.sh'
    echo
    echo '  # Run scenarios in parallel with `@test` tag'
    echo '  sh run-local-tests.sh -p -t test'
}

ARGS=

while getopts "t:ph" i
do
    case "$i"
    in
        p) ARGS="${ARGS} -Dparallel" ;;
        t) ARGS="${ARGS} -Dgroups=${OPTARG}" ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

BIN_DIR=${SCRIPT_DIR_PATH}/../dev/bin
INTEGRATION_TESTS_DIR=${SCRIPT_DIR_PATH}

. ${BIN_DIR}/configure.sh minikube-started
. ${LOCAL_ENV_FILE}

cd ${INTEGRATION_TESTS_DIR}

mvn clean verify -Pcucumber $ARGS