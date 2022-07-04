#!/bin/bash

########
# Create and configure a new minikube cluster
#
# Env vars:
# - MINIKUBE_PROFILE: set the current minikube profile (optional, default="minikube")
# - MINIKUBE_DRIVER: minikube driver (optional, default=auto detected by minikube itself)
# - MINIKUBE_CPUS: number of CPUs for minikube cluster (optional, default=4)
# - MINIKUBE_MEMORY: MB of RAM assigned to minikube cluster (optional, default=8192)
# - MINIKUBE_KUBERNETES_VERSION: Kubernetes version to use (optional, default="v1.20.0")
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

KUSTOMIZE_DIR="${SCRIPT_DIR_PATH}/../../kustomize"

disable_extra_components=$1

. "${SCRIPT_DIR_PATH}/configure.sh" minikube
stat "${SCRIPT_DIR_PATH}/../.." &> /dev/null || die "Can't cd to repository root"

minikube_opts=""
if [ -n "${MINIKUBE_DRIVER}" ]; then
  minikube_opts="${minikube_opts} --driver=${MINIKUBE_DRIVER}"
fi

if [ -n "${MINIKUBE_CONTAINER_RUNTIME}" ]; then
  minikube_opts="${minikube_opts} --container-runtime=${MINIKUBE_CONTAINER_RUNTIME}"
fi

set -x
minikube -p "${MINIKUBE_PROFILE}" ${minikube_opts} \
  --memory "${MINIKUBE_MEMORY}" \
  --cpus "${MINIKUBE_CPUS}" \
  "--kubernetes-version=${MINIKUBE_KUBERNETES_VERSION}" \
  start
sleep 30
minikube -p "${MINIKUBE_PROFILE}" addons enable ingress
sleep 5
minikube -p "${MINIKUBE_PROFILE}" addons enable ingress-dns
sleep 5

if [ "${disable_extra_components}" != 'true' ]; then
  kustomize build ${KUSTOMIZE_DIR}/overlays/minikube/keycloak | kubectl apply -f -
  sleep 5
  kubectl wait pod -l app-component=keycloak --for=condition=Ready --timeout=600s -n keycloak
  sleep 5
  kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/kube-prometheus/v0.9.0/manifests/setup/prometheus-operator-0servicemonitorCustomResourceDefinition.yaml
  . "${SCRIPT_DIR_PATH}/knative-installer.sh"
  yes | istioctl manifest apply --set profile=default --set values.gateways.istio-ingressgateway.type="ClusterIP"
  kubectl apply -f ${KUSTOMIZE_DIR}/overlays/minikube/istio/gateway.yaml
  kubectl apply -f ${KUSTOMIZE_DIR}/overlays/minikube/istio/virtual-service-kafka-broker.yaml
  kubectl apply -f ${KUSTOMIZE_DIR}/overlays/minikube/istio/jwt-request-authentication.yaml
  cat ${KUSTOMIZE_DIR}/overlays/minikube/istio/jwt-request-authentication.yaml | sed -E "s|<REPLACE_WITH_MINIKUBE_IP>|$(minikube -p "${MINIKUBE_PROFILE}" ip)|" | kubectl apply -f -
fi

sleep 5
minikube addons enable registry
sleep 5

kamel install --global
sleep 5

set +x
