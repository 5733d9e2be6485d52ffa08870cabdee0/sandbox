#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

printf "\n\nDeleting bridge: $BRIDGE_ID"
curl -s -H "Authorization: $OB_TOKEN" -X DELETE $MANAGER_URL/api/v1/bridges/$BRIDGE_ID
printf "\nDeleted"

