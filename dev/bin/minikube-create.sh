#!/bin/bash

########
# Create and configure a new minikube cluster
#
# Env vars:
# - MINIKUBE_PROFILE: set the current minikube profile (optional, default="minikube")
########

. "$( dirname "$0" )/configure.sh" minikube
cd "$( dirname "$0" )/../.." || die "Can't cd to repository root"

minikube -p "${MINIKUBE_PROFILE}" --driver=virtualbox --memory 8192 --cpus 4 --kubernetes-version=v1.20.0 start
sleep 30s
minikube -p "${MINIKUBE_PROFILE}" addons enable ingress
sleep 5s
minikube -p "${MINIKUBE_PROFILE}" addons enable ingress-dns
sleep 5s
kustomize build kustomize/overlays/minikube/keycloak | kubectl apply -f -
sleep 5s
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/kube-prometheus/v0.9.0/manifests/setup/prometheus-operator-0servicemonitorCustomResourceDefinition.yaml
