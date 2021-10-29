# DEMO

Requirements to run locally:
* docker-compose
* curl
* [jq](https://stedolan.github.io/jq/)    

The platform service is running on the endpoint specified in [this](https://docs.google.com/document/d/1C3s0ft4On8MIoi5v7dPXdsoNVCjLH8kMD2bdekHZ7Zg/edit?usp=sharing) gdoc (not publicly available yet), but this demo is usable also locally.

First of all, export the base address of the Manager. When running locally, the application by default will run on `localhost:8080`. If you want to use the staging area, refer to the link above.

```bash
export MANAGER_URL=http://localhost:8080
```

# Authentication

Each request will need a [Bearer](https://quarkus.io/guides/security#openid-connect) token passed as an http header. To get the token, run:

```shell
export OB_TOKEN="Bearer $(curl --insecure -X POST http://localhost:8180/auth/realms/event-bridge-fm/protocol/openid-connect/token     --user event-bridge:secret     -H 'content-type: application/x-www-form-urlencoded'     -d 'username=kermit&password=thefrog&grant_type=password' | jq --raw-output '.access_token')"
```

This token will last 3 minutes. Each time you get a `401 Unauthorized` from OpenBridge, run the command above again.

## How to create a Bridge instance

In order to send events to an Ingress, it is necessary to create a Bridge instance using the endpoint `/api/v1/bridges`. The request must include the name of the Bridge 

```json
{"name":  "myBridge"}
```

Run 

```bash
curl -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"name": "myBridge"}' $MANAGER_URL/api/v1/bridges | jq .
```

The response should look like something like

```json
{
  "kind":"Bridge",
  "id":"87508471-ee0f-4f53-b574-da8a61285986",
  "name":"myBridge",
  "href":"/api/v1/bridges/87508471-ee0f-4f53-b574-da8a61285986",
  "submitted_at":"2021-09-24T11:29:33.086649+0000",
  "status":"REQUESTED"
}
```

Extract the `id` field and store it in another env variable called `BRIDGE_ID`

```bash
export BRIDGE_ID=87508471-ee0f-4f53-b574-da8a61285986 # same id as before
```

Until the Bridge is not in the `AVAILABLE` state, it is not possible to create Processors and to push events to the Ingress. 
Check the status of the deployment with a GET request to the `/api/v1/bridges/{id}` endpoint: 

```bash
curl -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges/$BRIDGE_ID | jq .
```

the response should look like 

```json
{
  "kind":"Bridge",
  "id":"87508471-ee0f-4f53-b574-da8a61285986",
  "name":"myBridge",
  "href":"/api/v1/bridges/87508471-ee0f-4f53-b574-da8a61285986",
  "submitted_at":"2021-09-24T11:29:33.086649+0000",
  "status":"AVAILABLE",
  "endpoint":"/ingress/events/87508471-ee0f-4f53-b574-da8a61285986"
}
```

The application is now `AVAILABLE` and we also have the information about the endpoint to use to push the events: `/ingress/events/87508471-ee0f-4f53-b574-da8a61285986` in this particular case.

## How to add Processors to the Bridge

It is possible to add Processors to the Bridge instance using the endpoint `/api/v1/bridges/{bridgeId}/processors`. 
There are 3 basic concepts around a Processor:

- **filters**: when one or more filters are specified, only those events that evaluates the filters to `true` will be processed by the specified action. The documentation around the filters can be found [here](FILTERS.md).
- **actions**: the action to take with the filtered events.
- **transformations**: an optional transformation logic can be specified for each action definition.

Note that an action is always required by a Processor, but the filters and the transformation are not mandatory (i.e. the events will always flow into the action without filtering).

An example payload request is the following: 

```json
{
  "name": "myProcessor",
  "action": {
    "name": "myKafkaAction",
    "parameters": {"topic":  "demoTopic"},
    "type": "KafkaTopicAction"
  },
  "filters": [
    {
      "key": "source",
      "type": "StringEquals",
      "value": "StorageService"
    }
  ]
}
```

So only the events with `source` equals to `StorageService` will be sent to the 
the action `KafkaTopicAction`, which will push the event to the kafka instance under the topic `demoTopic`.

Run 

```bash
curl -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"name": "myProcessor", "action": {"name": "myKafkaAction", "parameters": {"topic":  "demoTopic"}, "type": "KafkaTopicAction"},"filters": [{"key": "source","type": "StringEquals","value": "StorageService"}]}' $MANAGER_URL/api/v1/bridges/$BRIDGE_ID/processors | jq .
```

and the response is something like 

```json
{
  "id":"cad90605-9836-4378-9250-f9c8d19f4e0c",
  "kind":"Processor",
  "href":"/api/v1/bridges/87508471-ee0f-4f53-b574-da8a61285986/processors/cad90605-9836-4378-9250-f9c8d19f4e0c",
  "name":"myProcessor",
  "bridge":{"kind":"Bridge","id":"87508471-ee0f-4f53-b574-da8a61285986","name":"myBridge","href":"/api/v1/bridges/87508471-ee0f-4f53-b574-da8a61285986","submitted_at":"2021-09-24T11:29:33.086649+0000","status":"AVAILABLE","endpoint":"/ingress/events/87508471-ee0f-4f53-b574-da8a61285986"},
  "submitted_at":"2021-09-24T11:49:47.170209+0000",
  "status":"REQUESTED",
  "filters": 
    [
      {
        "type":"StringEquals",
        "key":"source",
        "value":"StorageService"
      }
    ],
  "action":
    {
      "name":"myKafkaAction",
      "type":"KafkaTopicAction",
      "parameters":
      {
        "topic":"demoTopic"
      }
    }
}
```

Extract the Processor ID, it's the first id field in top level response. 

```bash
export PROCESSOR_ID=cad90605-9836-4378-9250-f9c8d19f4e0c
```

Like for the Bridge instance, it is needed to wait until the Processor has been deployed. Monitor its state with the endpoint `/api/v1/bridges/{bridgeId}/processors/{processorId}`

```bash
curl -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges/$BRIDGE_ID/processors/$PROCESSOR_ID  | jq .
```

## Send events to the Ingress

Everything is now set up to accept events and process them. Let's send the following cloud event to the ingress at `/ingress/events/{id}`. 

Here's the cloud event we are going to send:

```json
{
    "specversion": "1.0",
    "type": "Microsoft.Storage.BlobCreated",  
    "source": "StorageService",
    "id": "9aeb0fdf-c01e-0131-0922-9eb54906e209",
    "time": "2019-11-18T15:13:39.4589254Z",
    "subject": "blobServices/default/containers/{storage-container}/blobs/{new-file}",
    "dataschema": "#",
    "data": {
        "api": "PutBlockList",
        "clientRequestId": "4c5dd7fb-2c48-4a27-bb30-5361b5de920a",
        "requestId": "9aeb0fdf-c01e-0131-0922-9eb549000000",
        "eTag": "0x8D76C39E4407333",
        "contentType": "image/png",
        "contentLength": 30699,
        "blobType": "BlockBlob",
        "url": "https://gridtesting.blob.core.windows.net/testcontainer/{new-file}",
        "sequencer": "000000000000000000000000000099240000000000c41c18",
        "storageDiagnostics": {
            "batchId": "681fe319-3006-00a8-0022-9e7cde000000"
        }
    }
}
```

with the following request

```bash
curl -X POST -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{ "specversion": "1.0", "type": "Microsoft.Storage.BlobCreated", "source": "StorageService", "id": "9aeb0fdf-c01e-0131-0922-9eb54906e209", "time": "2019-11-18T15:13:39.4589254Z", "subject": "blobServices/default/containers/{storage-container}/blobs/{new-file}", "dataschema": "#", "data": { "api": "PutBlockList", "clientRequestId": "4c5dd7fb-2c48-4a27-bb30-5361b5de920a", "requestId": "9aeb0fdf-c01e-0131-0922-9eb549000000", "eTag": "0x8D76C39E4407333", "contentType": "image/png", "contentLength": 30699, "blobType": "BlockBlob", "url": "https://gridtesting.blob.core.windows.net/testcontainer/{new-file}", "sequencer": "000000000000000000000000000099240000000000c41c18", "storageDiagnostics": { "batchId": "681fe319-3006-00a8-0022-9e7cde000000"}}}' $MANAGER_URL/ingress/name/$BRIDGE_ID
```

if the event is a valid cloud event and everything went well, the server will return a response with status `200`.

