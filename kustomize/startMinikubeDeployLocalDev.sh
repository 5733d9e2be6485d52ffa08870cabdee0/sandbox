#!/bin/bash

credentials_dir="../dev/bin/credentials"
current_dir=$(pwd)

echo "Setup Managed Kafka and Managed Connectors"
cd ../dev/bin
. configure.sh managed-connectors
. kafka-setup.sh

cd $current_dir

echo "Starting Minikube"
minikube start
minikube addons enable ingress
minikube addons enable ingress-dns

echo "Applying IP replacements"
sed -i -E "s|(.*http://).*(:30007.*)|\1$(minikube ip)\2|" overlays/minikube/shard/patches/deploy-config.yaml
sed -i -E "s|(.*http://).*(:30007.*)|\1$(minikube ip)\2|" overlays/minikube/manager/patches/deploy-config.yaml
sleep 10s

echo "Deploying all resources"
kustomize build overlays/minikube | kubectl apply -f -

status=$?
if [ $status -ne 0 ]; then
  echo "Some resources fail to deploy (concurrency issues), redeploying"
  sleep 5s
  kustomize build overlays/minikube | kubectl apply -f -
fi

echo "Replace manager configuration with RHOAS info"
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_BOOTSTRAP_SERVERS=).*|\1$(rhoas kafka describe -o json | jq --raw-output '.bootstrap_server_host')|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_CLIENT_ID=).*|\1$( jq --raw-output '.clientID' $credentials_dir/$ops_sa_name.json )|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_KAFKA_CLIENT_SECRET=).*|\1$( jq --raw-output '.clientSecret' $credentials_dir/$ops_sa_name.json )|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_INSTANCE_API_HOST=https://admin-server-).*(/rest)|\1$(rhoas kafka describe -o json | jq --raw-output '.bootstrap_server_host')\2|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_ID=).*|\1$( jq --raw-output '.clientID' $credentials_dir/$admin_sa_name.json )|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_SECRET=).*|\1$( jq --raw-output '.clientSecret' $credentials_dir/$admin_sa_name.json )|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*RHOAS_OPS_ACCOUNT_CLIENT_ID=).*|\1$( jq --raw-output '.clientID' $credentials_dir/$ops_sa_name.json )|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_CLUSTER_ID=).*|\1$MANAGED_CONNECTORS_CLUSTER_ID|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_BOOTSTRAP_SERVERS=).*|\1$(rhoas kafka describe -o json | jq --raw-output '.bootstrap_server_host')|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_CLIENT_ID=).*|\1$( jq --raw-output '.clientID' $credentials_dir/$mc_sa_name.json )|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_KAFKA_CLIENT_SECRET=).*|\1$( jq --raw-output '.clientSecret' $credentials_dir/$mc_sa_name.json )|" overlays/minikube/manager/kustomization.yaml
sed -i -E "s|(.*MANAGED_CONNECTORS_AUTH_OFFLINE_TOKEN=).*|\1$OPENSHIFT_OFFLINE_TOKEN|" overlays/minikube/manager/kustomization.yaml

echo "Wait for Keycloak to start"
MINIKUBE_IP=$(minikube ip)
kubectl wait --for=condition=available --timeout=300s deployment/keycloak -n keycloak
timeout 120 bash -c 'while [[ "$(curl --insecure -s -o /dev/null -w ''%{http_code}'' http://'$MINIKUBE_IP':30007/auth)" != "303" ]]; do sleep 5; done'

echo "Configure shard operator technical bearer token"
TOKEN=$(curl --insecure -X POST http://$MINIKUBE_IP:30007/auth/realms/event-bridge-fm/protocol/openid-connect/token --user event-bridge:secret -H 'content-type: application/x-www-form-urlencoded' -d 'username=webhook-robot-1&password=therobot&grant_type=password&scope=offline_access' | jq --raw-output '.access_token')
sed -i -E "s|(.*WEBHOOK_TECHNICAL_BEARER_TOKEN=).*|\1$TOKEN|" overlays/minikube/shard/kustomization.yaml

echo "Redeploy resources to apply token"
kustomize build overlays/minikube | kubectl apply -f -
kubectl delete pod --selector=app=event-bridge-shard-operator -n event-bridge-operator

echo "Wait for manager and operator to start"
kubectl wait --for=condition=available --timeout=240s deployment/event-bridge-shard-operator -n event-bridge-operator
kubectl wait --for=condition=available --timeout=240s deployment/event-bridge -n event-bridge-manager
