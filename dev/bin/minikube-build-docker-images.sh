#!/bin/bash

########
# Build docker images and save them to minikube registry
#
# Env vars:
# - MINIKUBE_PROFILE: set the current minikube profile (optional, default="minikube")
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

. "${SCRIPT_DIR_PATH}/configure.sh" minikube

cd "${SCRIPT_DIR_PATH}/../.." || die "Can't cd to repository root"

env_command='docker-env'
container_engine_option=
if [ ! -z "${MINIKUBE_CONTAINER_RUNTIME}" ] && [ "${MINIKUBE_CONTAINER_RUNTIME}" != "docker" ]; then
  echo 'Use podman env'
  env_command='podman-env'
  container_engine_option='-Dquarkus.jib.docker-executable-name=podman'
fi

eval $( minikube -p "${MINIKUBE_PROFILE}" ${env_command} )
mvn clean install -Dquickly -Dquarkus.container-image.build=true -Dquarkus.jib.docker-executable-name=${container_engine}
