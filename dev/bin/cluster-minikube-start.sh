#!/bin/bash -e

########
# Create and configure a new minikube cluster
#
# Env vars:
# - MINIKUBE_PROFILE: set the current minikube profile (optional, default=minikube)
# - MINIKUBE_DRIVER: minikube driver (optional, default=auto detected by minikube itself)
# - MINIKUBE_CONTAINER_RUNTIME: minikube container runtime (optional, default=auto detected by minikube itself)
# - MINIKUBE_CPUS: number of CPUs for minikube cluster (optional, default=4)
# - MINIKUBE_MEMORY: MB of RAM assigned to minikube cluster (optional, default=8192)
# - MINIKUBE_KUBERNETES_VERSION: Kubernetes version to use (optional, default version can be found in the `$root/.env` file)
########

disable_extra_components=$1
if [ -n "$disable_extra_components" ]; then
    shift
fi

. $(dirname "${BASH_SOURCE[0]}")/common.sh

stat "${root_dir}" &> /dev/null || die "Can't cd to repository root"

configure_minikube

remove_local_env 'CLUSTER_IP'

minikube_opts=""
if [ -n "${minikube_driver}" ]; then
  minikube_opts="${minikube_opts} --driver=${minikube_driver}"
fi

if [ -n "${minikube_container_runtime}" ]; then
  minikube_opts="${minikube_opts} --container-runtime=${minikube_container_runtime}"
fi

set -x

if ! minikube -p "${minikube_profile}" ip; then
    minikube -p "${minikube_profile}" ${minikube_opts} \
        --memory "${minikube_memory}" \
        --cpus "${minikube_cpus}" \
        "--kubernetes-version=${minikube_kubernetes_version}" \
        start
        sleep 30
else
    echo "Minikube cluster already running. Updating the kubectl context."
    kubectl config use-context ${minikube_profile}
fi

minikube -p "${minikube_profile}" addons enable ingress
sleep 5
minikube -p "${minikube_profile}" addons enable ingress-dns
sleep 5

write_local_env 'CLUSTER_IP' "$( minikube -p "${minikube_profile}" ip )"

set +x