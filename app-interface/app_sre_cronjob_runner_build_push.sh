#!/bin/bash

IMAGE_NAME="quay.io/app-sre/rhose-cronjob-runner"
# the RHOSE CronJob Runner is stable, we do not need to push a new image for every commit
IMAGE_TAG="1.2"

docker build -f app-interface/cronjobs/Dockerfile.cronjobs -t "${IMAGE_NAME}:${IMAGE_TAG}"  .
docker tag "${IMAGE_NAME}:${IMAGE_TAG}" "${IMAGE_NAME}:latest"

DOCKER_CONF="${PWD}/.docker"
mkdir -p "${DOCKER_CONF}"
docker --config="${DOCKER_CONF}" login -u="${QUAY_USER}" -p="${QUAY_TOKEN}" quay.io

docker --config="${DOCKER_CONF}" push "${IMAGE_NAME}:${IMAGE_TAG}"
docker --config="${DOCKER_CONF}" push "${IMAGE_NAME}:latest"
