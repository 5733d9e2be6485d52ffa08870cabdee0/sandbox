#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

check_token
configure_manager

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
        "myMessage" : '"\"${MESSAGE}\""'
    }
}'


BRIDGE_ENDPOINT=$(curl -s -H "Authorization: Bearer $(get_token)" -X GET "${manager_url}/api/v1/bridges/${BRIDGE_ID}" | jq -r .endpoint)

echo "Sending cloud event to ${BRIDGE_ENDPOINT}"
curl -s -X POST -H 'Accept: application/json' -H 'Content-Type: application/json' -H "Authorization: Bearer $(get_token)" -d "${CLOUD_EVENT}" "${BRIDGE_ENDPOINT}"
echo "- Message ${MESSAGE} sent"
