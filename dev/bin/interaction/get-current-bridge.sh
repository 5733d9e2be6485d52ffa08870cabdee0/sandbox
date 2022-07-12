#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

curl -s -H "Authorization: $OB_TOKEN" -X GET "$MANAGER_URL/api/smartevents_mgmt/v1/bridges/$BRIDGE_ID" | jq .
