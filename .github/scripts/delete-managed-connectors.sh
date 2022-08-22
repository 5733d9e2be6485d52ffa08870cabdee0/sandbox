#!/bin/bash

# Script used to delete the Managed connectors
# Managed connectors are identified using "ci-" prefix in their names , which are created older than 12 hours and deleted
# Script uses the MANAGED_CONNECTORS_CONTROL_PLANE_URL env var from localconfig as a Connectors base path
# This requires the ocm command: https://github.com/openshift-online/ocm-cli
# You will need to login with `ocm login --token [...]`.

. $(dirname "${BASH_SOURCE[0]}")/../configure.sh

export COS_BASE_PATH=${MANAGED_CONNECTORS_CONTROL_PLANE_URL}/api/connector_mgmt/v1
export CONNECTORS_BASE=${COS_BASE_PATH}/kafka_connectors

MANAGED_CONNECTORS_IDS=$(curl -L --insecure --oauth2-bearer "$(ocm token)" "${CONNECTORS_BASE}" | jq -r '(now - 12*60*60 | strflocaltime("%Y-%m-%dT%H:%M:%S")) as $older  | .items[] | select(.name | startswith("ci-")) | select(.owner="openbridge_kafka_supporting") | select((.created_at | sub("-(?<d>[0-9]-)";"-0\(.d)")) <= $older) | .id')

for ID in $MANAGED_CONNECTORS_IDS
do
  curl -L --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr -XDELETE "${CONNECTORS_BASE}"/"${ID}" | jq
  echo "Deleted Managed Connector with id ${ID}"
done
