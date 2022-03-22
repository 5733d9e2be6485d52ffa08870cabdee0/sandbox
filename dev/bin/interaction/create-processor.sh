#!/bin/bash

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

PROCESSOR_NAME=${1:-$TODAY_PROCESSOR_NAME}

export PROCESSOR_CREATION='{
   "name": '"\"$PROCESSOR_NAME\""',
   "action": {
      "type": "Slack",
      "parameters": {
         "channel": "mc",
         "webhookUrl": '"\"$SLACK_WEBHOOK_URL\""'
      }
   },
  "filters": [
    {
      "key": "source",
      "type": "StringEquals",
      "value": "StorageService"
    }
  ],
  "transformationTemplate": "{\"test\": \"{data.myMessage}\"}"
}'

export PROCESSOR_CREATION_WEBHOOK='{
   "name": '"\"$PROCESSOR_NAME\""',
   "action": {
      "type": "Webhook",
      "parameters": {
         "endpoint": '"\"$SLACK_WEBHOOK_URL\""'
      }
   },
  "filters": [
    {
      "key": "source",
      "type": "StringEquals",
      "value": "StorageService"
    }
  ],
  "transformationTemplate": "{\"text\": \"{data.myMessage}\"}"
}'

printf "\n\nCreating the processor with name $PROCESSOR_NAME\n"
PROCESSOR_ID=$(curl -s -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "$PROCESSOR_CREATION_WEBHOOK" $MANAGER_URL/api/v1/bridges/$BRIDGE_ID/processors | jq -r .id)

printf "\n\nProcessor Created: $PROCESSOR_NAME\n"
echo "export PROCESSOR_ID=$PROCESSOR_ID"
