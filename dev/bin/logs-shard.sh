#!/bin/bash -e

########
# Read the shard operator logs
########

. $(dirname "${BASH_SOURCE[0]}")/common.sh

configure_cluster_started

if [ "${DEV_ENVIRONMENT}" = 'true' ]; then
    tail -f ${dev_logs_dir}/shard-operator.log
else
    pod_name=$(kubectl get pods -n event-bridge-operator | grep event-bridge-shard-operator | grep Running | awk '{print $1}' | head -n 1)
    if [ -z ${pod_name} ]; then
        echo 'Cannot find the manager pod...'
        echo
        kubectl get pods -n event-bridge-operator
        echo
        echo 'Please review...'
        exit 1
    else
        kubectl logs -f ${pod_name} -n event-bridge-operator
    fi
fi