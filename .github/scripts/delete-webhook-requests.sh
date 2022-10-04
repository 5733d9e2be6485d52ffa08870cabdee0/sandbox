#!/bin/bash

# Script used to delete the WebHook site requests which are older than 24 hours

webhook_base_path=https://webhook.site
token=$1
end_date=$(date "--date=now -1day" +"%Y-%m-%d%%20%H:%M:%S")

if [[ -z "$token" ]]
then
    echo "Mandatory Webhook UUID parameter not provided"
    exit 1
fi

uuids=$(curl --location -S -s -D /dev/stderr $webhook_base_path/token/$token/requests?date_to=$end_date | jq '.data[].uuid' | tr -d \")

curl_status=$?
if [ $curl_status -ne 0 ]; then
  echo "Failed to fetch Webhook requests"
  exit $curl_status
fi

for uuid in $uuids
do
  curl --location -S -s -D /dev/stderr -XDELETE $webhook_base_path/token/$token/request/$uuid

  curl_status=$?
  if [ $curl_status -eq 0 ]; then
    echo "Deleted UUID $uuid"
  else
    echo "Failed to delete Webhook request"
    exit $curl_status
  fi
done
