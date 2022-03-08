#!/bin/bash

script_dir_path=$(dirname "${BASH_SOURCE[0]}")

. ${script_dir_path}/../dev/bin/common.sh

usage() {
    echo 'Usage: run-local-tests.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -t $TAGS            Tags to use for selecting scenario'
    echo '  -p                  To be set if you want to run the tests in parallel mode'
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

while getopts "t:pkh" i
do
    case "$i"
    in
        p) ARGS="${ARGS} -Dparallel" ;;
        t) ARGS="${ARGS} -Dgroups=${OPTARG}" ;;
        k) ARGS="${ARGS} -Dcleanup.disable" ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

integration_tests_dir=`realpath ${script_dir_path}`

configure_cluster_started
configure_manager
configure_keycloak


export KEYCLOAK_CLIENT_ID=event-bridge
export KEYCLOAK_CLIENT_SECRET=secret
export KEYCLOAK_TOKEN_OFFLINE_ACCESS=false
export KEYCLOAK_USERNAME=kermit
export KEYCLOAK_PASSWORD=thefrog

export OB_TOKEN=$(get_keycloak_access_token)

if [ -f "${dev_config_dir}/testconfig" ]; then
    while IFS= read -r line; do
        export ${line}
    done < "${dev_config_dir}/testconfig"
fi

set -x # activate printing executed commands

mvn clean verify $ARGS \
  -f ${integration_tests_dir}/pom.xml \
  -Pcucumber \
  -Devent-bridge.manager.url=${manager_url} \
  -Dkeycloak.realm.url=${keycloak_url}/auth/realms/event-bridge-fm

set +x
