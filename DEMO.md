# Demo of Event Bridge

If running this demo against your local machine, please ensure you have completed all steps in the local dev
setup documentation: [dev/README.md](dev/README.md)

Alternatively, the demo can be run against our "demo cluster". Details are specified in 
[this](https://docs.google.com/document/d/1C3s0ft4On8MIoi5v7dPXdsoNVCjLH8kMD2bdekHZ7Zg/edit?usp=sharing) gdoc (not publicly available yet). If running against our "demo cluster", you
will need to update the URLs used in the rest of this guide to match.

The following assumes you will be running the demo on your local machine:

First of all, export the base address of the Manager. When running locally, the application by default will run on `localhost:8080`. If you want to use the staging area, refer to the link above.
If you deployed the infrastructure with minikube, the keycloak server is running under `http://<YOUR_MINIKUBE_IP>:30007`.

```bash
export MANAGER_URL=http://localhost:8080
export KEYCLOAK_URL=http://`minikube ip`:30007
```

Or, if you are using the [dev/README.md](dev/README.md) environment, you can also simply run:

```bash
.  dev/bin/credentials/local_env
```

# Authentication

Each request will need a [Bearer](https://quarkus.io/guides/security#openid-connect) token passed as a http header. To get the token, run:

```shell
export OB_TOKEN="Bearer $(curl --insecure -X POST $KEYCLOAK_URL/auth/realms/event-bridge-fm/protocol/openid-connect/token --user event-bridge:secret -H 'content-type: application/x-www-form-urlencoded' -d 'username=kermit&password=thefrog&grant_type=password' | jq --raw-output '.access_token')"
```

This token will last 3 minutes. Each time you get a `401 Unauthorized` from EventBridge, run the command above again.

## Testing your Setup

This is a good time to test your setup. To do this we will invoke the `/api/v1/bridges` endpoint of EventBridge to the
currently running Bridge Instances. This _will_ return an empty list, but it will demonstrate that the Fleet Manager is
working.

Use the following to test the API:

```bash
curl -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges | jq .
```

You should get a response that looks like this:

```json
{"kind":"BridgeList","items":[],"page":0,"size":0,"total":0}
```

If not, please double-check the setup instructions in [dev/README.md](dev/README.md) to ensure everything is running
as expected.

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
  "status":"accepted"
}
```

Extract the `id` field and store it in another env variable called `BRIDGE_ID`

```bash
export BRIDGE_ID=87508471-ee0f-4f53-b574-da8a61285986 # same id as before
```

Until the Bridge is not in the `ready` state, it is not possible to create Processors and to push events to the Ingress. 
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
  "status":"ready",
  "endpoint":"http://ob-87508471-ee0f-4f53-b574-da8a61285986-ob-kekkobar.apps.openbridge-dev.fdvn.p1.openshiftapps.com/"
}
```

Keep track of the `endpoint`, it will be used later when pushing an event to this ingress application.

```bash
export BRIDGE_ENDPOINT="$(curl -H "Authorization: $OB_TOKEN" -X GET $MANAGER_URL/api/v1/bridges/$BRIDGE_ID | jq --raw-output .endpoint)"
```

The application is now `ready` and we also have the information about the endpoint to use to push the events: `http://ob-87508471-ee0f-4f53-b574-da8a61285986-ob-kekkobar.apps.openbridge-dev.fdvn.p1.openshiftapps.com/` in this particular case. The paths to submit events are
1. `/events`: it accepts only valid cloud event json payloads.
2. `/events/plain`: it accepts any json string as payload, but it is mandatory to specify the headers `ce-specversion`, `ce-type`, `ce-id`, `ce-source` and `ce-subject`. 

## How to add Processors to the Bridge

It is possible to add `Processors` to the Bridge instance using the endpoint `/api/v1/bridges/{bridgeId}/processors`. 
There are 3 basic concepts around a Processor:

- `Filters`: when one or more `Filters` are specified, only those events that evaluates the `Filters` to `true` will be processed by the specified `Action`.
  - Further documentation on the supported `Filters` can be found [here](FILTERS.md).
- `Actions`: the `Action` to take with the filtered events.
  - Further documentation on the supported `Actions` can be found [here](ACTIONS.md)
- `Transformations`: an optional `Transformation` logic can be specified for each action definition.
  - Further documention on writing `Transformations` can be found [here](TRANSFORMATIONS.md)

Note that an `Action` is always required by a `Processor`, but `Filters` and the `Transformation` are optional (i.e. the events will always flow into the action without filtering).

An example payload request is the following: 

```json
{
  "name": "myProcessor",
  "action": {
    "name": "myKafkaAction",
    "parameters": {"topic":  "demoTopic"},
    "type": "KafkaTopic"
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
the action `KafkaTopic`, which will push the event to the kafka instance under the topic `demoTopic`.

Run 

```bash
curl -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{"name": "myProcessor", "action": {"name": "myKafkaAction", "parameters": {"topic":  "demoTopic"}, "type": "KafkaTopic"},"filters": [{"key": "source","type": "StringEquals","value": "StorageService"}]}' $MANAGER_URL/api/v1/bridges/$BRIDGE_ID/processors | jq .
```

and the response is something like 

```json
{
  "id":"cad90605-9836-4378-9250-f9c8d19f4e0c",
  "kind":"Processor",
  "href":"/api/v1/bridges/87508471-ee0f-4f53-b574-da8a61285986/processors/cad90605-9836-4378-9250-f9c8d19f4e0c",
  "name":"myProcessor",
  "submitted_at":"2021-09-24T11:49:47.170209+0000",
  "status":"accepted",
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
      "type":"KafkaTopic",
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

Everything is now set up to accept events and process them. Let's send the following cloud event to the ingress at the endpoint you extracted from the Bridge response: in this example `http://ob-87508471-ee0f-4f53-b574-da8a61285986-ob-kekkobar.apps.openbridge-dev.fdvn.p1.openshiftapps.com/events`. 

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

with the following request (change the url according to your ingress application endpoint)

```bash
curl -X POST -H 'Accept: application/json' -H 'Content-Type: application/json' -H "Authorization: $OB_TOKEN" -d '{ "specversion": "1.0", "type": "Microsoft.Storage.BlobCreated", "source": "StorageService", "id": "9aeb0fdf-c01e-0131-0922-9eb54906e209", "time": "2019-11-18T15:13:39.4589254Z", "subject": "blobServices/default/containers/{storage-container}/blobs/{new-file}", "dataschema": "#", "data": { "api": "PutBlockList", "clientRequestId": "4c5dd7fb-2c48-4a27-bb30-5361b5de920a", "requestId": "9aeb0fdf-c01e-0131-0922-9eb549000000", "eTag": "0x8D76C39E4407333", "contentType": "image/png", "contentLength": 30699, "blobType": "BlockBlob", "url": "https://gridtesting.blob.core.windows.net/testcontainer/{new-file}", "sequencer": "000000000000000000000000000099240000000000c41c18", "storageDiagnostics": { "batchId": "681fe319-3006-00a8-0022-9e7cde000000"}}}' "$BRIDGE_ENDPOINT/events"
```

if the event is a valid cloud event and everything went well, the server will return a response with status `200`.

