#!/bin/bash -e

########
# Load all resources via kustomize.
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
configure_images

echo "Patch bridge executors and ingresses"

set -x

executors="$(kubectl get bridgeexecutors -A | grep -v NAME)"
while read -r executor
do
    namespace=$(echo ${executor} | awk '{print $1}')
    name=$(echo ${executor} | awk '{print $2}')
    echo "Patch executor ${name} in namespace ${namespace} with image ${executor_container_name}"
    kubectl patch bridgeexecutor ${name} -n ${namespace} --patch '{"spec": {"image": "'${executor_container_name}'"}}' --type=merge
done < <(kubectl get bridgeexecutors -A | grep -v NAME)

ingresses=$(kubectl get bridgeingresses -A | grep -v NAME)
while read -r ingress
do
    namespace=$(echo ${ingress} | awk '{print $1}')
    name=$(echo ${ingress} | awk '{print $2}')
    echo "Patch ingress ${name} in namespace ${namespace} with image ${ingress_container_name}"
    kubectl patch bridgeingress ${name} -n ${namespace} --patch '{"spec": {"image": "'${ingress_container_name}'"}}' --type=merge
done < <(kubectl get bridgeingresses -A | grep -v NAME)