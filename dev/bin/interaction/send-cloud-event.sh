#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

BRIDGE_NAME=${1:-$TODAY_BRIDGE_NAME}

MESSAGE="'$(date +%H:%M:%S)'-my new message"

export CLOUD_EVENT='{
    "specversion": "1.0",
    "type": "Microsoft.Storage.BlobCreated",
    "source": "StorageService",
    "id": "9aeb0fdf-c01e-0131-0922-9eb54906e209",
    "time": "2019-11-18T15:13:39.4589254Z",
    "subject": "blobServices/default/containers/{storage-container}/blobs/{new-file}",
    "dataschema": "#",
    "data": {
        "traceMessage" : '"\"$MESSAGE\""',
        "myMessage" : '"\"$MESSAGE\""',
        "genus":"Citrullus",
        "name":"Watermelon",
        "id":25,
        "family":"Cucurbitaceae",
        "order":"Cucurbitales",
        "nutritions":
          {"carbohydrates":8,
            "protein":0.6,
            "fat":0.2,
            "calories":30,
            "sugar": '"${1:-6}"'
          }

    }
}'


echo $CLOUD_EVENT | jq .

BRIDGE_ENDPOINT=$(curl -s -H "Authorization: $OB_TOKEN" -X GET "$MANAGER_URL/api/smartevents_mgmt/v1/bridges/$BRIDGE_ID" | jq -r .endpoint)

echo "Sending cloud event to $BRIDGE_ENDPOINT"
curl -s -X POST -H 'Accept: application/json' -H 'Content-Type: application/cloudevents+json' -H "Authorization: $OB_TOKEN" -d "$CLOUD_EVENT" "$BRIDGE_ENDPOINT"
echo "- Message $MESSAGE sent"
