Feature: End to End Bridge integration tests

  Scenario:By default Manager url should not be accessible without authentication
    Given get list of Bridge instances returns HTTP response code 401

  Scenario: Bridge is created and in available state
    Given get list of Bridge instances with access token doesn't contain Bridge "notificationBridge"
    When create a Bridge with name "notificationBridge" with access token
    Then get list of Bridge instances with access token contains Bridge "notificationBridge"
    Then get Bridge with access token exists in status "AVAILABLE" within 2 minutes

  Scenario: Processor gets created to the bridge and deployed
    When add Processor to the Bridge with access token:
    """
    {
      "name": "myProcessor",
      "action": {
        "name": "myKafkaAction",
        "parameters": {
            "topic":  "myKafkaTopic"
            },
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
    """

    When add invalid Processor to the Bridge with access token returns HTTP response code 400:
    """
    {
      "name": "processorInvalid"
       "filters": [
        {
        "key": "source",
        "type": "StringEquals",
        "value": "StorageService"
        }
      ]
    }
    """
    And get Processor with access token exists in status "AVAILABLE" within 3 minutes

    And send cloud events to the ingress at the endpoint with access token:
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
    """

  Scenario: Bridge, Processor and Ingress gets deleted
    When the Processor is deleted
    Then the Processor doesn't exists within 2 minutes

    When delete a Bridge
    Then the Bridge doesn't exists within 2 minutes

    And the Ingress is Undeployed within 1 minute

  Scenario: Verify Metrics details exist
    Given the Metrics info is exists
