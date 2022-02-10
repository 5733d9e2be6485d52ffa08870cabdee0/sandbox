#!/bin/bash

########
# Build docker images and save them to minikube registry
#
# Env vars:
# - MINIKUBE_PROFILE: set the current minikube profile (optional, default="minikube")
########

. "$( dirname "$0" )/configure.sh" minikube
cd "$( dirname "$0" )/../.." || die "Can't cd to repository root"

eval $( minikube -p "${MINIKUBE_PROFILE}" docker-env )
mvn clean install -DskipTests -Dquarkus.container-image.build=true
