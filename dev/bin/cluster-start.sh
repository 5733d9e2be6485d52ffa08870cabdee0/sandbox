#!/bin/bash -e

########
# Create and configure a new cluster
# This script will decide which `cluster-*-start.sh` script to run
# For the environment setup of starting a cluster, please refer to the corresponding `cluster-*-start.sh` script
########

. $(dirname "${BASH_SOURCE[0]}")/common.sh

configure_cluster

if [ "${cluster_type}" = 'minikube' ]; then
    echo "Starting Minikube"    
    ${dev_bin_dir}/cluster-minikube-start.sh
elif [ "${cluster_type}" = 'kind' ]; then
    echo "Starting Kind"
    ${dev_bin_dir}/cluster-kind-start.sh
else
    echo "ERROR: Unsupported cluster ${cluster_type}. Please use `minikube` or `kind`."
    exit 1
fi