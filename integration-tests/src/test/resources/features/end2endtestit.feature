Feature: End to End Bridge integration tests

  Scenario: Manager is not accessible without authentication
    Given the list of Bridge instances is failing with HTTP response code 401

  Scenario: Bridge is created and correctly deleted
    Given authenticate against Manager

    When create a new Bridge "mybridge"
    And the list of Bridge instances is containing the Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes
    
    When delete the Bridge "mybridge"

    Then the Bridge "mybridge" is not existing within 2 minutes
    And the Ingress of Bridge "mybridge" is not available within 1 minute

    And the Manager metric 'manager_bridge_status_change_total{status="PROVISIONING",}' count is at least 1
    And the Manager metric 'manager_bridge_status_change_total{status="READY",}' count is at least 1
    And the Manager metric 'manager_bridge_status_change_total{status="DELETED",}' count is at least 1


  Scenario: Processor is created, deployed and correctly deleted
    Given authenticate against Manager
    
    When create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "parameters": {
            "topic":  "myKafkaTopic"
            },
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
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And the Processor "myProcessor" of the Bridge "mybridge" has action of type "KafkaTopicAction" and parameters:
      | topic | myKafkaTopic |

    And send a cloud event to the Ingress of the Bridge "mybridge":
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
    And the Ingress of the Bridge "mybridge" metric 'http_server_requests_seconds_count{method="POST",outcome="SUCCESS",status="200",uri="/events",}' count is at least 1
    # TODO
    # And the Ingress of the Bridge "mybridge" metric 'http_server_requests_seconds_count{method="POST",outcome="SUCCESS",status="200",uri="/events/plain",}' count is at least 1
    And delete the Processor "myProcessor" of the Bridge "mybridge"
    Then the Processor "myProcessor" of the Bridge "mybridge" is not existing within 2 minutes

    And the Manager metric 'manager_processor_status_change_total{status="PROVISIONING",}' count is at least 1
    And the Manager metric 'manager_processor_status_change_total{status="READY",}' count is at least 1
    And the Manager metric 'manager_processor_status_change_total{status="DELETED",}' count is at least 1

  Scenario: Processor payload is malformed
    Given authenticate against Manager
    
    When create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes
    
    Then add a Processor to the Bridge "mybridge" and returns HTTP response code 400 with body:
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
