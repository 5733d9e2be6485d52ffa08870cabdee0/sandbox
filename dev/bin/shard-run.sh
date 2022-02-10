#!/bin/bash

########
# Run Fleet Shard locally in dev mode
#
# Env vars:
# - MANAGED_KAFKA_INSTANCE_NAME: set the managed kafka instance name (required)
########

. "$( dirname "$0" )/configure.sh" kafka minikube-started

echo "Retrieving webhook technical bearer token..."

keycloak_response=$(
    curl --insecure \
      -X POST "http://${minikube_ip}:30007/auth/realms/event-bridge-fm/protocol/openid-connect/token" \
      --user event-bridge:secret \
      -H 'content-type: application/x-www-form-urlencoded' \
      -d 'username=webhook-robot-1&password=therobot&grant_type=password&scope=offline_access'
  ) || die "Failed to retrieve token from keycloak. Is minikube running?"

event_bridge_webhook_technical_bearer_token=$( echo -n "${keycloak_response}"| jq --raw-output -e '.access_token') \
  || die "Failed to retrieve token from keycloak. Is minikube running?"

echo "Webhook technical bearer token retrieved: ${event_bridge_webhook_technical_bearer_token}"

mvn \
  -Devent-bridge.webhook.technical-bearer-token=${event_bridge_webhook_technical_bearer_token} \
  -Dminikubeip=${minikube_ip} \
  -Dquarkus.http.port=1337 \
  -Pminikube \
  -f "$( dirname "$0" )/../../shard-operator/pom.xml" \
  clean compile quarkus:dev $@
