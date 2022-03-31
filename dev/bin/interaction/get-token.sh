#!/bin/bash
# This needs to be called with source get-token.sh

if [[ "$TARGET_ENVIRONMENT" == "local" ]]; then
  export OB_TOKEN="Bearer $(curl -s --insecure -X POST $KEYCLOAK_URL/auth/realms/event-bridge-fm/protocol/openid-connect/token --user event-bridge:secret -H 'content-type: application/x-www-form-urlencoded' -d "username=$KEYCLOAK_USERNAME&password=$KEYCLOAK_PASSWORD&grant_type=password" | jq --raw-output '.access_token')"
elif [[ "$TARGET_ENVIRONMENT" == "remote" ]]; then
  export OB_TOKEN="Bearer $(curl -s --insecure -X POST $SSO_REDHAT_URL/auth/realms/redhat-external/protocol/openid-connect/token --header 'Content-Type: application/x-www-form-urlencoded' --data-urlencode 'client_id=cloud-services' --data-urlencode 'grant_type=refresh_token' --data-urlencode "refresh_token=$OPENSHIFT_OFFLINE_TOKEN" | jq --raw-output '.access_token')"
else
  echo "Unknown value for TARGET_ENVIRONMENT. Valid values are [local, remote]"
  return 1
fi
echo '- Token Set'
