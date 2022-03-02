#!/bin/sh

source "bin/common.sh"

BRIDGE_NAME=${1:-$TODAY_BRIDGE_NAME}

export CLOUD_EVENT='{
    "specversion": "1.0",
    "type": "Microsoft.Storage.BlobCreated",
    "source": "StorageService",
    "id": "9aeb0fdf-c01e-0131-0922-9eb54906e209",
    "time": "2019-11-18T15:13:39.4589254Z",
    "subject": "blobServices/default/containers/{storage-container}/blobs/{new-file}",
    "dataschema": "#",
    "data": {
        "myMessage" : "'$(date +%H:%M:%S)'-my new message"
    }
}'


BRIDGE_ENDPOINT=$(http $MANAGER_URL/api/v1/bridges/$BRIDGE_ID Authorization:"$OB_TOKEN" | jq -r .endpoint)

curl -v -X POST -H 'Accept: application/json' -H 'Content-Type: application/json' -H "Authorization: $OB_TOKEN" -d "$CLOUD_EVENT" "$BRIDGE_ENDPOINT/events"
