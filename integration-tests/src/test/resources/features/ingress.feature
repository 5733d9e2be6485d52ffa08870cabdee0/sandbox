@test
Feature: Ingress tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge"
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
      }
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes

  Scenario: Send Cloud Event
    When send a cloud event to the Ingress of the Bridge "mybridge" with path "events":
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
    Then the Ingress of the Bridge "mybridge" metric 'http_server_requests_seconds_count{method="POST",outcome="SUCCESS",status="200",uri="/events",}' count is at least 1

  Scenario: Send plain Cloud Event
    When send a cloud event to the Ingress of the Bridge "mybridge" with path "events/plain" and default headers:
    """
    { "data" : "test" }
    """
    Then the Ingress of the Bridge "mybridge" metric 'http_server_requests_seconds_count{method="POST",outcome="SUCCESS",status="200",uri="/events/plain",}' count is at least 1

  Scenario: Send plain Cloud Event without headers
    Then send a cloud event to the Ingress of the Bridge "mybridge" with path "events/plain" is failing with HTTP response code 400:
    """
    { "data" : "test" }
    """

  Scenario: Send Cloud Event to wrong path    
    Then send a cloud event to the Ingress of the Bridge "mybridge" with path "ingress/not-the-bridge-name/" is failing with HTTP response code 404:
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

  Scenario: Send Cloud Event without authentication
    When logout of Manager
    
    Then send a cloud event to the Ingress of the Bridge "mybridge" with path "events" is failing with HTTP response code 401:
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

  Scenario: Send plain Cloud Event without authentication
    When logout of Manager
    
    Then send a cloud event to the Ingress of the Bridge "mybridge" with path "events/plain" and default headers is failing with HTTP response code 401:
    """
    { "data" : "test" }
    """