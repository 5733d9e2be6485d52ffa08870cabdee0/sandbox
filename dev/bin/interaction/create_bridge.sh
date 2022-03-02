#!/bin/sh

source "bin/common.sh"

BRIDGE_NAME=${1:-$TODAY_BRIDGE_NAME}

# Create the bridge
printf "\n\nCreating the bridge with name $BRIDGE_NAME"
NEW_BRIDGE_ID=$(curl -s -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "{\"name\": \"$BRIDGE_NAME\"}" $MANAGER_URL/api/v1/bridges | jq -r .id)

printf "Bridge $BRIDGE_NAME created, set the bridge id\n\n"
echo "export BRIDGE_ID=$NEW_BRIDGE_ID"
printf "\n"

