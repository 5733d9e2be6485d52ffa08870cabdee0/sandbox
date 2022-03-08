#!/bin/bash -e

########
# Stop a running cluster
# This script will decide which `cluster-*-stop.sh` script to run
# For the environment setup of stopping a cluster, please refer to the corresponding `cluster-*-stop.sh` script
########

. $(dirname "${BASH_SOURCE[0]}")/common.sh

configure_cluster

if [ "${cluster_type}" = 'minikube' ]; then
    echo "Stopping Minikube"    
    ${dev_bin_dir}/cluster-minikube-stop.sh
elif [ "${cluster_type}" = 'kind' ]; then
    echo "Stopping Kind"
    ${dev_bin_dir}/cluster-kind-stop.sh
else
    echo "ERROR: Unsupported cluster ${cluster_type}. Please use `minikube` or `kind`."
    exit 1
fi