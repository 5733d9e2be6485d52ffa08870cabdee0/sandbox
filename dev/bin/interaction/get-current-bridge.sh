#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

check_token
configure_manager

curl -s -H "Authorization: Bearer $(get_token)" -X GET "${manager_url}/api/v1/bridges/${BRIDGE_ID}" | jq .
