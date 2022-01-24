#!/bin/sh

# import utils functions
dev_dir="$(dirname "$0")"
. $dev_dir/utils/utils.sh

function run_shard(){
  local webhook_tech_token=$1
  local minikube_ip=$2

  mvn clean compile \
    -f $dev_dir/../shard-operator/pom.xml \
    -Dquarkus.http.port=1337 \
    -Dminikubeip=$minikube_ip \
    -Devent-bridge.webhook.technical-bearer-token=$webhook_tech_token \
    -Pminikube \
    quarkus:dev
}

function retrieve_webhook_tech_token(){
  local minikube_ip=$1
  KEYCLOAK_RESPONSE=$(curl --insecure -X POST http://$minikube_ip:30007/auth/realms/event-bridge-fm/protocol/openid-connect/token --user event-bridge:secret -H 'content-type: application/x-www-form-urlencoded' -d 'username=webhook-robot-1&password=therobot&grant_type=password&scope=offline_access')
  if [ ! $? -eq 0 ]; then
    echo "Failed to retrieve token from keycloak. Please make sure that you installed keycloak on minikube and that it is running fine" 1>&2
    return 1
  fi

  EVENT_BRIDGE_WEBHOOK_TECHNICAL_BEARER_TOKEN=$(echo $KEYCLOAK_RESPONSE | jq --raw-output -e '.access_token')
  if [ ! $? -eq 0 ]; then
    echo "Failed to extract token from keycloak response. Please make sure the credentils this script is using are correct with your keycloak realm configuration." 1>&2
    return 1
  fi
  echo $EVENT_BRIDGE_WEBHOOK_TECHNICAL_BEARER_TOKEN
  return 0
}

function main(){
  MINIKUBE_IP=$(minikube ip)
  if ! valid_ip $MINIKUBE_IP ; then
      echo "Can not retrieve minikube ip. Please make sure that minikube is running fine." 1>&2
      return 1
  fi
  echo "Minikube is up and running at "$MINIKUBE_IP

  EVENT_BRIDGE_WEBHOOK_TECHNICAL_BEARER_TOKEN=$(retrieve_webhook_tech_token $MINIKUBE_IP)

  if [ ! $? -eq 0 ]; then
    exit 1
  fi

  echo "Starting the shard.."
  run_shard $MINIKUBE_IP $EVENT_BRIDGE_WEBHOOK_TECHNICAL_BEARER_TOKEN
}

main