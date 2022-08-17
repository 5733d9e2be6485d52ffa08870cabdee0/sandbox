#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

BRIDGE_NAME=$TODAY_BRIDGE_NAME
bridge_error_type='none'

usage() {
    echo 'Usage: create-bridge.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -n                  Bridge name. Default is the generated '"$TODAY_BRIDGE_NAME"
    echo '  -e                  Bridge error type. Default is `none`. Available values: none (no error handling), webhook (send the error to webhook), kafka (send the error to kafka)'
    echo
    echo 'Examples:'
    echo '  # Create default slack action'
    echo '  sh create-bridge.sh'
    echo
    echo '  # Create "example_bridge" bridge with error handling of type webhook'
    echo '  sh create-bridge.sh -n "example_bridge" -e webhook'
}

while getopts "e:n:h" i
do
    case "$i"
    in
        n) BRIDGE_NAME="${OPTARG}" ;;
        e) bridge_error_type="${OPTARG}" ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

if [ "${bridge_error_type}" = "none" ]; then
    bridge_payload='{"name": '"\"$BRIDGE_NAME\""', "cloud_provider": "aws", "region": "us-east-1" }'
elif [ "${bridge_error_type}" = 'webhook' ]; then
    bridge_payload='{"name": '"\"$BRIDGE_NAME\""', "cloud_provider": "aws", "region": "us-east-1",
                    "error_handler": { "type": "webhook_sink_0.1",
                                                     "parameters": {
                                                         "endpoint": '"\"$ERROR_HANDLER_WEBHOOK_URL\""'
                                                     }
                                                   }}'
elif [ "${bridge_error_type}" = 'kafka' ]; then
      bridge_payload='{"name": '"\"$BRIDGE_NAME\""', "cloud_provider": "aws", "region": "us-east-1",
                      "error_handler": {
      "type": "kafka_topic_sink_0.1",
      "parameters": {
         "topic": '"\"$KAFKA_ERROR_TOPIC\""',
         "kafka_broker_url": '"\"$KAFKA_BROKER_URL\""',
         "kafka_client_id": '"\"$KAFKA_CLIENT_ID\""',
         "kafka_client_secret": '"\"$KAFKA_CLIENT_SECRET\""'
      }
   }}'
fi



echo $bridge_payload | jq

# Create the bridge
printf "\n\nCreating the bridge with name $BRIDGE_NAME"
NEW_BRIDGE_ID=$(curl -s -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "$bridge_payload" $MANAGER_URL/api/smartevents_mgmt/v1/bridges | jq -r .id)

printf "\nBridge $BRIDGE_NAME created, set the bridge id\n\n"
echo "\texport BRIDGE_ID=$NEW_BRIDGE_ID"
printf "\n"

