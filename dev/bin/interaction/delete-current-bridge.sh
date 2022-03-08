#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

printf "Deleting bridge: $BRIDGE_ID\n"
curl -s -H "Authorization: Bearer $(get_token)" -X DELETE $MANAGER_URL/api/v1/bridges/$BRIDGE_ID
printf "\nBridge deleted\n"

