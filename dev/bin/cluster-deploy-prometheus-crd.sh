#!/bin/bash -e

########
# Load prometheus crds resources via kustomize on cluster.
# Kustomize directory can be changed via first arg of the script
########

deploy_dir=$1
if [ -n "$deploy_dir" ]; then
    shift
fi

. $(dirname "${BASH_SOURCE[0]}")/common.sh

if [ -z ${deploy_dir} ]; then
    deploy_dir=${kustomize_deploy_dir}
fi

configure_cluster_started

echo "Deploy Prometheus CRDs to ${cluster_type} cluster"
kustomize build ${deploy_dir}/overlays/minikube/prometheus | kubectl apply -f -