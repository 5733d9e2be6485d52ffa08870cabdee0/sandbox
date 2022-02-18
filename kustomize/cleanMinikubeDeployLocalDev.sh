#!/bin/bash

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

KUSTOMIZE_DIR="${SCRIPT_DIR_PATH}/../kustomize"
BIN_DIR="${SCRIPT_DIR_PATH}/../dev/bin"
DEPLOY_DIR="${KUSTOMIZE_DIR}/_deploy"
KUSTOMIZE_DEPLOY_DIR="${DEPLOY_DIR}/kustomize"

. ${BIN_DIR}/configure.sh minikube-started

cd "${KUSTOMIZE_DEPLOY_DIR}" || die "Can't access deployed dir. Did you start the minikube with `startMinikubeDeployLocalDev.sh` script ?"

echo "Remove all Openbridge namespaces"
# This could be replaced by a read in manager of all OB instances running and wait for all of them to be deleted
kubectl get ns | grep "Active" | awk -F " " '{print $1}' | grep "^ob-" | xargs kubectl delete ns || echo "nothing to delete"

echo "Removing all resources"
kustomize build overlays/minikube | kubectl delete -f -

sleep 30