#!/bin/bash

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

# Script used to delete Bridges and Processors used in E2E tests
# Script will delete Bridges without Processors, for Bridges with Processors it will delete Processors if they are in proper state
# Test Bridges are identified using "test-" prefix in their names

# If Bridge contains Processors then only Processors are deleted by the script execution. If user wants to delete also Bridges then the script has to be executed again after some time (so the Processors can be removed first)!

BRIDGES=($(curl -s -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges | jq '.items[] | select(.name | startswith("test-")) | .id' | tr -d \"))

for i in "${BRIDGES[@]}"
do
   NUMBER_OF_PROCESSORS=$(curl -s -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges/$i/processors | jq '.items | length')
  if [ "$NUMBER_OF_PROCESSORS" == "0" ]; then
    echo "No processors in Bridge $i."
    BRIDGE_STATUS=$(curl -s -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges/$i | jq '.status' | tr -d \")
    if [ "$BRIDGE_STATUS" == "ready" ] || [ "$BRIDGE_STATUS" == "failed" ]; then
      echo "Deleting Bridge $i"
      curl -s -H "Authorization: $OB_TOKEN" -X DELETE $MANAGER_URL/api/v1/bridges/$i
    else
      echo "Bridge $i status $BRIDGE_STATUS is incompatible with automated deletion, please remove it manually"
    fi
  else
    echo "$NUMBER_OF_PROCESSORS processors found in Bridge $i"
    PROCESSORS=($(curl -s -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges/$i/processors | jq '.items[].id' | tr -d \"))

    for j in "${PROCESSORS[@]}"
    do
      PROCESSOR_STATUS=$(curl -s -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges/$i/processors/$j | jq '.status' | tr -d \")
      if [ "$PROCESSOR_STATUS" == "ready" ] || [ "$PROCESSOR_STATUS" == "failed" ]; then
        echo "Deleting Processor $j"
        curl -s -H "Authorization: $OB_TOKEN" -X DELETE $MANAGER_URL/api/v1/bridges/$i/processors/$j
      else
        echo "Processor $j status $PROCESSOR_STATUS is incompatible with automated deletion, please remove it manually"
      fi
    done

    echo "Bridge $i is not deleted, rerun the script after all Processors above are deleted to delete the Bridge!"
  fi
done

