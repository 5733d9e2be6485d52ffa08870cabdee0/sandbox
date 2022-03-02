#!/bin/sh

source "bin/common.sh"

printf "\n\nDeleting processor $PROCESSOR_NAME: $PROCESSOR_ID"
curl -s -H "Authorization: $OB_TOKEN" -X DELETE $MANAGER_URL/api/v1/bridges/$BRIDGE_ID/processors/$PROCESSOR_ID

