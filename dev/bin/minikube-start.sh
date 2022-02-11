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

. "$( dirname "$0" )/configure.sh" minikube
cd "$( dirname "$0" )/../.." || die "Can't cd to repository root"

minikube_driver_flag=""
if [ -n "${MINIKUBE_DRIVER}" ]; then
  minikube_driver_flag="--driver=${MINIKUBE_DRIVER}"
fi

minikube -p "${MINIKUBE_PROFILE}" "${minikube_driver_flag}" \
  --memory "${MINIKUBE_MEMORY}" \
  --cpus "${MINIKUBE_CPUS}" \
  "--kubernetes-version=${MINIKUBE_KUBERNETES_VERSION}" \
  start
sleep 30s
minikube -p "${MINIKUBE_PROFILE}" addons enable ingress
sleep 5s
minikube -p "${MINIKUBE_PROFILE}" addons enable ingress-dns
sleep 5s
kustomize build kustomize/overlays/minikube/keycloak | kubectl apply -f -
sleep 5s
kubectl wait pod -l app-component=keycloak --for=condition=Ready --timeout=600s -n keycloak
sleep 5s
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/kube-prometheus/v0.9.0/manifests/setup/prometheus-operator-0servicemonitorCustomResourceDefinition.yaml
