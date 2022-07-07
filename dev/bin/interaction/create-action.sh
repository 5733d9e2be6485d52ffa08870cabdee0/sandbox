#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
action_name=$TODAY_ACTION_NAME
action_type='slack'

usage() {
    echo 'Usage: create-action.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -n                  Action name. Default is the generated $TODAY_ACTION_NAME'
    echo '  -t                  Action type. Default is `slack`. Available values: slack (usable with send-cloud-event.sh), slack-plain (without filtering or transformation), webhook'
    echo
    echo 'Examples:'
    echo '  # Create default slack action'
    echo '  sh create-action.sh'
    echo
    echo '  # Create action_name action of type webhook type'
    echo '  sh create-action.sh -n action_name -t webhook'
}

while getopts "t:n:h" i
do
    case "$i"
    in
        n) action_name="${OPTARG}" ;;
        t) action_type="${OPTARG}" ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

if [ "${action_type}" = 'slack' ]; then
  action_payload='{
   "name": '"\"$action_name\""',
   "action": {
      "type": "slack_sink_0.1",
      "parameters": {
         "slack_channel": "mc",
         "slack_webhook_url": '"\"$SLACK_WEBHOOK_URL\""'
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
elif [ "${action_type}" = 'camel' ]; then
  action_payload='{
  "name": '"\"$action_name\""',
  "action": {
    "name": "mySlackAction",
    "type": "slack_sink_0.1",
    "parameters": {
      "slack_channel": "mc",
      "slack_webhook_url": '"\"$SLACK_WEBHOOK_URL\""'
    }
  },
  "processing": {
      "type": "cameldsl_0.1",
      "spec": {
        "flow": {
          "from": {
            "uri": "rhose",
            "steps": [
              {
                "to": "mySlackAction"
              }
            ]
          }
        }
      }
    },
    "actions": [
      {
        "name": "mySlackAction",
        "type": "slack_sink_0.1",
        "parameters": {
          "slack_channel": "mc",
          "slack_webhook_url": '"\"$SLACK_WEBHOOK_URL\""'
        }
      }
    ],
    "transformationTemplate": "{\"text\": \"{data.myMessage}\"}"
}'
elif [ "${action_type}" = 'slack-plain' ]; then
  action_payload='{
   "name": '"\"$action_name\""',
   "action": {
      "type": "slack_sink_0.1",
      "parameters": {
         "slack_channel": "mc",
         "slack_webhook_url": '"\"$SLACK_WEBHOOK_URL\""'
      }
   }
}'
elif [ "${action_type}" = 'webhook' ]; then
  action_payload='{
   "name": '"\"$action_name\""',
   "action": {
      "type": "webhook_sink_0.1",
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
elif [ "${action_type}" = 'kafka' ]; then
  action_payload='{
   "name": '"\"$action_name\""',
   "action": {
      "type": "kafka_topic_sink_0.1",
      "parameters": {
         "topic": '"\"$KAFKA_TOPIC\""',
         "kafka_broker_url": '"\"$KAFKA_BROKER_URL\""',
         "kafka_client_id": '"\"$KAFKA_CLIENT_ID\""',
         "kafka_client_secret": '"\"$KAFKA_CLIENT_SECRET\""'
      }
   },
  "transformationTemplate": "{\"text\": \"{data.myMessage}\"}"
}'
else
  echo "Unknown action type: ${action_type}"
  usage
  exit 1
fi


printf "\n\nCreating the ${action_type} action with name $action_name\n"
PROCESSOR_ID=$(curl -s -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "$action_payload" $MANAGER_URL/api/v1/bridges/$BRIDGE_ID/processors | jq -r .id)

printf "\n\nAction ${action_type} created: $action_name\n"
echo "export PROCESSOR_ID=$PROCESSOR_ID"
