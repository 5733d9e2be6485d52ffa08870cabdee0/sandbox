#!/bin/bash

# Script used to delete the Managed connectors
# Managed connectors are identified using "ci-" prefix in their names which older than 12 hours and deleted

. $(dirname "${BASH_SOURCE[0]}")/../configure.sh

export COS_BASE_PATH=${MANAGED_CONNECTORS_CONTROL_PLANE_URL}/api/connector_mgmt/v1
export CONNECTORS_BASE=${COS_BASE_PATH}/kafka_connectors

MC_IDS=$(curl -L --insecure --oauth2-bearer "$(ocm token)" "${CONNECTORS_BASE}" | jq '(now - 12*60*60 | strflocaltime("%Y-%m-%dT%H:%M:%S")) as $recent  | .items[] | select(.name | startswith("ci-"))| select((.created_at | sub("-(?<d>[0-9]-)";"-0\(.d)")) <= $recent)| .id')

for id in $MC_IDS
 do
  curl -L --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr -XDELETE "${COS_BASE_PATH}"/"${ID}" | jq
  echo "Deleted Managed Connector with id ${id}"
done
