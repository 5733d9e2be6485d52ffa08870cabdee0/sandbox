#!/bin/bash

########
# Run Fleet Shard locally in dev mode
#
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

export MANAGED_KAFKA_INSTANCE_NAME=rhose-local-development

. "${SCRIPT_DIR_PATH}/configure.sh" kafka minikube-started

mvn \
  -Dminikubeip=${MINIKUBE_IP} \
  -Ddebug=5006 \
  -Dquarkus.http.port=1337 \
  -Devent-bridge.logging.json=false \
  -Devent-bridge.k8s.orchestrator=minikube \
  -Devent-bridge.istio.gateway.name=rhose-ingressgateway \
  -Devent-bridge.istio.gateway.namespace=istio-system \
  -Devent-bridge.webhook.technical-account-id=402cf429-da04-4931-8089-e53ad452192b \
  -Devent-bridge.istio.jwt.issuer=http://${MINIKUBE_IP}:30007/auth/realms/event-bridge-fm \
  -Devent-bridge.istio.jwt.jwksUri=http://keycloak.keycloak:8180/auth/realms/event-bridge-fm/protocol/openid-connect/certs \
  -f "${SCRIPT_DIR_PATH}/../../shard-operator-parent/shard-operator/pom.xml" \
  clean compile quarkus:dev $@
