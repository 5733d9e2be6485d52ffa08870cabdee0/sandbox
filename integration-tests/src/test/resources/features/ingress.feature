Feature: Ingress tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge" in cloud provider "aws" and region "us-east-1"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes
    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "type": "webhook_sink_0.1",
        "parameters": {
            "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
        }
      }
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes

  Scenario: Send Cloud Event
    When send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
    "specversion": "1.0",
    "type": "Microsoft.Storage.BlobCreated",
    "source": "StorageService",
    "id": "9aeb0fdf-c01e-0131-0922-9eb54906e209",
    "time": "2019-11-18T15:13:39.4589254Z",
    "subject": "blobServices/default/containers/{storage-container}/blobs/{new-file}",
    "dataschema": "#",
    "data": {
        "api": "PutBlockList"
      }
    }
    """

  Scenario: Send plain Cloud Event
    When send a json event to the Ingress of the Bridge "mybridge" with headers "Ce-Id":"my-id","Ce-Source":"mySource","Ce-Specversion":"1.0","Ce-Type":"myType":
    """
    { "data" : "test" }
    """