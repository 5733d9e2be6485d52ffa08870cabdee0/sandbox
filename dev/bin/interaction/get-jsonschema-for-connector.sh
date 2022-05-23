#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

CONNECTOR_NAME=$1

curl -s -H "Authorization: $OB_TOKEN" -X GET "$MANAGER_URL/api/v1/bridges/jsonschema/$CONNECTOR_NAME" | jq .
