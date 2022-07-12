#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

BRIDGE_NAME=${1:-$TODAY_BRIDGE_NAME}


if [ "$ERROR_HANDLER_WEBHOOK_URL" = "" ];
then
    bridge_payload='{"name": '"\"$BRIDGE_NAME\""' }'
else
    bridge_payload='{"name": '"\"$BRIDGE_NAME\""',
                    "error_handler": { "type": "webhook_sink_0.1",
                                                     "parameters": {
                                                         "endpoint": '"\"$ERROR_HANDLER_WEBHOOK_URL\""'
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

