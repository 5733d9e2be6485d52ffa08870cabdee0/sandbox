#!/bin/bash
# This needs to be called with source get-token.sh

source $(dirname "${BASH_SOURCE[0]}")/common.sh

if [[ "${TARGET_ENVIRONMENT}" == "local" ]]; then
  INTERACTION_OB_TOKEN=$(get_keycloak_access_token)
elif [[ "${TARGET_ENVIRONMENT}" == "remote" ]]; then
  INTERACTION_OB_TOKEN=$(get_sso_access_token)
else
  echo "Unknown value for TARGET_ENVIRONMENT. Valid values are [local, remote]"
  return 1
fi

write_local_env 'INTERACTION_OB_TOKEN' "${INTERACTION_OB_TOKEN}"

echo '- Token Set'
