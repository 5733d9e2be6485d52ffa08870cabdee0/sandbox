#!/bin/bash

IMAGE_NAME="quay.io/app-sre/rhose-fleet-manager"
IMAGE_TAG=$(git rev-parse HEAD)

docker build -f docker/Dockerfile.appsre -t "${IMAGE_NAME}:${IMAGE_TAG}"  .
docker tag "${IMAGE_NAME}:${IMAGE_TAG}" "${IMAGE_NAME}:latest"

DOCKER_CONF="${PWD}/.docker"
mkdir -p "${DOCKER_CONF}"
docker --config="${DOCKER_CONF}" login -u="${QUAY_USER}" -p="${QUAY_TOKEN}" quay.io

docker --config="${DOCKER_CONF}" push "${IMAGE_NAME}:${IMAGE_TAG}"
docker --config="${DOCKER_CONF}" push "${IMAGE_NAME}:latest"
