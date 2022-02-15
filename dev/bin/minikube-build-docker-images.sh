#!/bin/bash

########
# Build docker images and save them to minikube registry
#
# Env vars:
# - MINIKUBE_PROFILE: set the current minikube profile (optional, default="minikube")
########

. "${SCRIPT_DIR_PATH}/configure.sh" minikube

cd "${SCRIPT_DIR_PATH}/../.." || die "Can't cd to repository root"

eval $( minikube -p "${MINIKUBE_PROFILE}" docker-env )
mvn clean install -Dquickly -Dquarkus.container-image.build=true
