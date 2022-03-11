#!/bin/bash

set -exv

IMAGE_TAG=`git rev-parse --short=7 HEAD`
IMAGE_NAME="quay.io/app-sre/rhose-fleet-manager"

if [[ -z "$QUAY_USER" || -z "$QUAY_TOKEN" ]]; then
    echo "QUAY_USER and QUAY_TOKEN must be set"
    exit 1
fi

DOCKER_CONF="$PWD/.docker"
mkdir -p "$DOCKER_CONF"
docker --config="$DOCKER_CONF" login -u="$QUAY_USER" -p="$QUAY_TOKEN" quay.io
docker --config="$DOCKER_CONF" build -f ../docker/Dockerfile.jvm -t "${IMAGE_NAME}:latest" ../manager
docker --config="$DOCKER_CONF" tag "${IMAGE_NAME}:latest" "${IMAGE_NAME}:${IMAGE_TAG}"
docker --config="$DOCKER_CONF" push "${IMAGE_NAME}:latest"
docker --config="$DOCKER_CONF" push "${IMAGE_NAME}:${IMAGE_TAG}"

