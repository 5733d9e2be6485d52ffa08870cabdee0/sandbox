#!/bin/bash -e

########
# Stop the kind cluster
#
# Env vars:
# - KIND_NAME: set the current minikube profile (optional, default none)
########

. $(dirname "${BASH_SOURCE[0]}")/common.sh 

configure_kind

kind_opts=""
if [ -n "${KIND_NAME}" ]; then
  kind_opts="${kind_opts} --name ${KIND_NAME}"
fi

if [ -n "${KIND_CONTAINER_ENGINE}" ]; then
    export KIND_EXPERIMENTAL_PROVIDER=${KIND_CONTAINER_ENGINE}
fi

set -x
kind delete cluster ${kind_opts}

remove_local_env 'CLUSTER_IP'

set +x