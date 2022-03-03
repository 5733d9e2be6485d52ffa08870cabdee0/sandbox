#!/bin/bash
# This needs to be called with source get_token.sh

export OB_TOKEN="Bearer $(curl -s --insecure -X POST $KEYCLOAK_URL/auth/realms/event-bridge-fm/protocol/openid-connect/token --user event-bridge:secret -H 'content-type: application/x-www-form-urlencoded' -d "username=$KEYCLOAK_USERNAME&password=$KEYCLOAK_PASSWORD&grant_type=password" | jq --raw-output '.access_token')"

echo '- Token Set'