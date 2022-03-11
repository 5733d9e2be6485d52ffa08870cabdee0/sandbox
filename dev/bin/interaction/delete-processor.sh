#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

printf "\n\nDeleting processor $PROCESSOR_NAME: $PROCESSOR_ID"
curl -s -H "Authorization: $OB_TOKEN" -X DELETE $MANAGER_URL/api/v1/bridges/$BRIDGE_ID/processors/$PROCESSOR_ID

