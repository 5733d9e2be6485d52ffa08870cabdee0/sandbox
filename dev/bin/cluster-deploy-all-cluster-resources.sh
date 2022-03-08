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

configure_keycloak
configure_cluster_started
configure_images

remove_local_env 'MANAGER_URL'

echo "Setup images"
current_dir=$(pwd)
cd ${deploy_dir}/overlays/minikube
kustomize edit set image event-bridge-manager=${fleet_manager_container_name}
kustomize edit set image event-bridge-shard-operator=${fleet_shard_container_name}
cd ${current_dir}
sed -i -E "s|(.*EVENT_BRIDGE_EXECUTOR_IMAGE:).*|\1 ${executor_container_name}|" ${deploy_dir}/overlays/minikube/shard/patches/deploy-config.yaml
sed -i -E "s|(.*EVENT_BRIDGE_INGRESS_IMAGE:).*|\1 ${ingress_container_name}|" ${deploy_dir}/overlays/minikube/shard/patches/deploy-config.yaml
sed -i -E "s|(.*INGRESS_OVERRIDE_HOSTNAME:).*|\1 ${cluster_ip}|" ${deploy_dir}/overlays/minikube/shard/patches/deploy-config.yaml

echo "Replace manager configuration with RHOAS info"
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_BOOTSTRAP_SERVERS=).*|\1$( get_managed_kafka_bootstrap_server )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_CLIENT_ID=).*|\1$( get_managed_kafka_ops_sa_client_id )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_CLIENT_SECRET=).*|\1$( get_managed_kafka_ops_sa_client_secret )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_INSTANCE_API_HOST=https://admin-server-).*(/rest)|\1$( get_managed_kafka_bootstrap_server )\2|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_ID=).*|\1$( get_managed_kafka_admin_sa_client_id )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_SECRET=).*|\1$( get_managed_kafka_admin_sa_client_secret )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*RHOAS_OPS_ACCOUNT_CLIENT_ID=).*|\1$( get_managed_kafka_ops_sa_client_id )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_NAMESPACE_ID=).*|\1${MANAGED_CONNECTORS_NAMESPACE_ID}|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_BOOTSTRAP_SERVERS=).*|\1$( get_managed_kafka_bootstrap_server )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_CLIENT_ID=).*|\1$( get_managed_kafka_mc_sa_client_id )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_CLIENT_SECRET=).*|\1$( get_managed_kafka_mc_sa_client_secret )|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_AUTH_OFFLINE_TOKEN=).*|\1${OPENSHIFT_OFFLINE_TOKEN}|" ${deploy_dir}/overlays/minikube/manager/kustomization.yaml

echo "Apply resources"
kustomize build ${deploy_dir}/overlays/minikube | kubectl apply -f -

status=$?
if [ $status -ne 0 ]; then
    echo "WARNING: Some resources fail to deploy (concurrency issues), redeploying"
    sleep 5
    kustomize build ${deploy_dir}/overlays/minikube | kubectl apply -f -
fi

write_local_env 'MANAGER_URL' "http://${cluster_ip}:80/manager"