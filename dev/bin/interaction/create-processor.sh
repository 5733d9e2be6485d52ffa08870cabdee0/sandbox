#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

check_token
configure_manager

processor_name=$TODAY_PROCESSOR_NAME
processor_type='slack'

usage() {
    echo 'Usage: create-processor.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -n                  Processor name. Default is the generated $TODAY_PROCESSOR_NAME'
    echo '  -t                  Processor type. Default is `slack`. Available values: slack, webhook'
    echo
    echo 'Examples:'
    echo '  # Create default slack processor'
    echo '  sh create-processor.sh'
    echo
    echo '  # Create processor_name processor of type webhook type'
    echo '  sh create-processor.sh -n processor_name -t webhook'
}

while getopts "t:n:h" i
do
    case "$i"
    in
        n) processor_name="${OPTARG}" ;;
        t) processor_type="${OPTARG}" ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

processor_payload=
if [ "${processor_type}" = 'slack' ]; then
  processor_payload='{
   "name": '"\"$processor_name\""',
   "action": {
      "type": "Slack",
      "parameters": {
         "channel": "mc",
         "webhookUrl": '"\"${SLACK_WEBHOOK_URL}\""'
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
elif [ "${processor_type}" = 'webhook' ]; then
  processor_payload='{
   "name": '"\"$processor_name\""',
   "action": {
      "type": "Webhook",
      "parameters": {
         "endpoint": '"\"${SLACK_WEBHOOK_URL}\""'
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
else
  echo "Unknown processor type: ${processor_type}"
  usage
  exit 1
fi

echo $processor_payload

printf "\n\nCreating the ${processor_type} processor with name $processor_name\n"
PROCESSOR_ID=$(curl -s -X POST -H "Authorization: Bearer $(get_token)" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "${processor_payload}" ${manager_url}/api/v1/bridges/${BRIDGE_ID}/processors | jq -r .id)

printf "\n\nProcessor ${processor_type} created: $processor_name\n"
printf "\texport PROCESSOR_ID=$PROCESSOR_ID"
printf "\n\n"
