#!/bin/bash

########
# Deploy webhook-perf-test application to Minikube
########
eval $(minikube -p minikube docker-env)

kubectl create namespace performance

mvn \
  -f "$( dirname "$0" )/../../pom.xml" \
  clean package -DskipTests -Pminikube

kubectl wait deployment --all --timeout=600s --for=condition=Available -n performance

echo "Application is now available in minikube at" $(minikube service -n performance --url webhook-perf-test)"/webhook/events"