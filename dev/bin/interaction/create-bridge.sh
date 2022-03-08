#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

check_token
configure_manager

BRIDGE_NAME=${1:-${TODAY_BRIDGE_NAME}}

# Create the bridge
printf "\n\nCreating the bridge with name ${BRIDGE_NAME}"
NEW_BRIDGE_ID=$(curl -s -X POST -H "Authorization: Bearer $(get_token)" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "{\"name\": \"${BRIDGE_NAME}\"}" ${manager_url}/api/v1/bridges | jq -r .id)

printf "\nBridge ${BRIDGE_NAME} created, set the bridge id\n\n"
printf "\texport BRIDGE_ID=${NEW_BRIDGE_ID}"
printf "\n\n"

