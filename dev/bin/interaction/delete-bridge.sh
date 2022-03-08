#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

check_token
configure_manager

printf "\n\nDeleting bridge ${BRIDGE_ID}"
curl -s -H "Authorization: Bearer $(get_token)" -X DELETE ${manager_url}/api/v1/bridges/${BRIDGE_ID}
