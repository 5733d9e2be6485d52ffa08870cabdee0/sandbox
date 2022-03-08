#!/bin/bash -e

########
# Load keycloak resources via kustomize on cluster.
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

remove_local_env 'KEYCLOAK_URL'
remove_local_env 'IS_KEYCLOAK_INTERNAL_URL'

echo "Deploy keycloak to ${cluster_type} cluster"
kustomize build ${deploy_dir}/overlays/minikube/keycloak | kubectl apply -f -

keycloak_url="http://${cluster_ip}:30007"

echo "Wait for Keycloak to start"
kubectl wait --for=condition=available --timeout=300s deployment/keycloak -n keycloak
timeout 120 bash -c 'while [[ "$(curl --insecure -s -o /dev/null -w ''%{http_code}'' '${keycloak_url}'/auth)" != "303" ]]; do sleep 5; done'

is_keycloak_internal_url=false
if [ "${cluster_ip}" = 'localhost' ]; then
    # We cannot reach host localhost from the pod
    # happens with KIND
    # so we need to provide an internal url
    keycloak_url="http://keycloak.keycloak.svc.cluster.local:8180"
    is_keycloak_internal_url=true
fi

echo "Applying Keycloak IP replacements to shard and manager"
sed -i -E "s|(.*)http://.*:30007(.*)|\1${keycloak_url}\2|" ${kustomize_deploy_dir}/overlays/minikube/shard/patches/deploy-config.yaml
sed -i -E "s|(.*)http://.*:30007(.*)|\1${keycloak_url}\2|" ${kustomize_deploy_dir}/overlays/minikube/manager/patches/deploy-config.yaml

write_local_env 'KEYCLOAK_URL' ${keycloak_url}
write_local_env 'IS_KEYCLOAK_INTERNAL_URL' ${is_keycloak_internal_url}