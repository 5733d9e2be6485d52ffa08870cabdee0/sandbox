#!/bin/bash

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

KUSTOMIZE_DIR="${SCRIPT_DIR_PATH}/../kustomize"
BIN_DIR="${SCRIPT_DIR_PATH}/../dev/bin"
DEPLOY_DIR="${KUSTOMIZE_DIR}/_deploy"
KUSTOMIZE_DEPLOY_DIR="${DEPLOY_DIR}/kustomize"

. ${BIN_DIR}/configure.sh minikube-started kafka managed-connectors

echo "Removing all resources"
kustomize build ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube | kubectl delete -f -

status=$?
if [ $status -ne 0 ]; then
  echo "Some resources fail to deploy (concurrency issues), redeploying"
  sleep 5
  kustomize build ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube | kubectl apply -f -
fi

echo "Replace manager configuration with RHOAS info"
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_BOOTSTRAP_SERVERS=).*|\1$( getManagedKafkaBootstrapServerHost )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_CLIENT_ID=).*|\1$( getManagedKafkaOpsSAClientId )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_CLIENT_SECRET=).*|\1$( getManagedKafkaOpsSAClientSecret )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_INSTANCE_API_HOST=https://admin-server-).*(/rest)|\1$( getManagedKafkaBootstrapServerHost )\2|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_ID=).*|\1$( getManagedKafkaAdminSAClientId )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_SECRET=).*|\1$( getManagedKafkaAdminSAClientSecret )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*RHOAS_OPS_ACCOUNT_CLIENT_ID=).*|\1$( getManagedKafkaOpsSAClientId )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_CLUSTER_ID=).*|\1${MANAGED_CONNECTORS_CLUSTER_ID}|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_BOOTSTRAP_SERVERS=).*|\1$( getManagedKafkaBootstrapServerHost )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_CLIENT_ID=).*|\1$( getManagedKafkaMcSAClientId )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_CLIENT_SECRET=).*|\1$( getManagedKafkaMcSAClientSecret )|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_AUTH_OFFLINE_TOKEN=).*|\1${OPENSHIFT_OFFLINE_TOKEN}|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/manager/kustomization.yaml

echo "Wait for Keycloak to start"
kubectl wait --for=condition=available --timeout=300s deployment/keycloak -n keycloak
timeout 120 bash -c 'while [[ "$(curl --insecure -s -o /dev/null -w ''%{http_code}'' http://'${MINIKUBE_IP}':30007/auth)" != "303" ]]; do sleep 5; done'

echo "Configure shard operator technical bearer token"
TOKEN=$( getKeycloakAccessToken )
sed -i -E "s|(.*WEBHOOK_TECHNICAL_BEARER_TOKEN=).*|\1${TOKEN}|" ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube/shard/kustomization.yaml

echo "Redeploy resources to apply token"
kustomize build ${KUSTOMIZE_DEPLOY_DIR}/overlays/minikube | kubectl apply -f -
kubectl delete pod --selector=app=event-bridge-shard-operator -n event-bridge-operator

echo "Wait for manager and operator to start"
kubectl wait --for=condition=available --timeout=240s deployment/event-bridge-shard-operator -n event-bridge-operator
kubectl wait --for=condition=available --timeout=240s deployment/event-bridge -n event-bridge-manager
