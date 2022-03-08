#!/bin/bash -e

########
# Create and configure a new kind cluster
#
# Env vars:
# - KIND_NAME: set the current minikube profile (optional, default none)
# - KIND_CONFIG_FILE: kind config file (https://kind.sigs.k8s.io/docs/user/quick-start/#configuring-your-kind-cluster) (optional. Default is `dev/config/kind/config.yaml`)
# - KIND_CONTAINER_ENGINE: container engine to use (optional, default is docker)
# - KIND_NETWORK: If you want to use a different network from the default Kind one (optional)
# - KIND_KUBERNETES_VERSION: Kubernetes version to use (optional, default version can be found in the `$root/.env` file)
########

disable_extra_components=$1
if [ -n "$disable_extra_components" ]; then
    shift
fi

. $(dirname "${BASH_SOURCE[0]}")/common.sh

stat "${root_dir}" &> /dev/null || die "Can't access repository root"

configure_kind

remove_local_env 'CLUSTER_IP'

kind_opts=""

if [ -n "${kind_kubernetes_version}" ]; then
  kind_opts="${kind_opts} --image kindest/node:${kind_kubernetes_version}"
fi

if [ -n "${kind_config_file}" ]; then
  kind_opts="${kind_opts} --config ${kind_config_file}"
fi

if [ -n "${kind_container_engine}" ]; then
    export KIND_EXPERIMENTAL_PROVIDER=${kind_container_engine}
fi

set -x

if ! kind get clusters | grep ${kind_name}; then
    kind create cluster --name ${kind_name} ${kind_opts}
    sleep 30
else
    echo "Kind cluster already running"
fi

if [ "${kind_network}" = 'calico' ]; then
    # Source https://alexbrand.dev/post/creating-a-kind-cluster-with-calico-networking/
    kubectl apply -f https://projectcalico.docs.tigera.io/manifests/calico.yaml
    kubectl -n kube-system set env daemonset/calico-node FELIX_IGNORELOOSERPF=true
    sleep 5
    kubectl wait pod -l k8s-app=calico-node --for=condition=Ready --timeout=600s -n kube-system
fi

kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
sleep 5
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=300s

write_local_env 'CLUSTER_IP' 'kind-control-plane'

if ! cat /etc/hosts | grep kind-control-plane; then
    echo 'Please add the `kind-control-plane` to your hosts file. Example: `echo "127.0.0.1 kind-control-plane" | sudo tee -a /etc/hosts`'
fi

set +x