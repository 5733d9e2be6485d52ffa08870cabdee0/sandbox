#!/bin/bash

########
# Run Fleet Shard locally in dev mode
#
# Env vars:
# - MANAGED_KAFKA_INSTANCE_NAME: set the managed kafka instance name (required)
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

. "${SCRIPT_DIR_PATH}/configure.sh" kafka minikube-started

echo "Retrieving webhook technical bearer token..."

event_bridge_webhook_technical_bearer_token=$( getKeycloakAccessToken ) || die "Failed to retrieve token from keycloak. Is minikube running?"  

echo "Webhook technical bearer token retrieved: ${event_bridge_webhook_technical_bearer_token}"

mvn \
  -Devent-bridge.webhook.technical-bearer-token=${event_bridge_webhook_technical_bearer_token} \
  -Dminikubeip=${MINIKUBE_IP} \
  -Dquarkus.http.port=1337 \
  -Pminikube \
  -f "${SCRIPT_DIR_PATH}/../../shard-operator/pom.xml" \
  clean compile quarkus:dev $@
