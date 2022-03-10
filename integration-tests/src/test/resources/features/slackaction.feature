Feature: Slack Action tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

  Scenario: Slack Action Processor is created and slack message should be received in the slack channel
    Then add a Slack Action Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "slackProcessor",
      "action": {
        "type": "Slack",
        "parameters": {
            "channel": "mc"
            }
      },
      "filters": [
        {
        "key": "source",
        "type": "StringEquals",
        "value": "StorageService"
        }
       ],
  	  "transformationTemplate": "{data.myMessage}"
    }
    """
    And the Processor "slackProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And the Processor "slackProcessor" of the Bridge "mybridge" has action of type "Slack"

    And send a slack message cloud event to the Ingress of the Bridge "mybridge" with path "events":
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
    }
    }
    """
    And verify slack message is received in the slack app

  Scenario: Slack Action Processor creation returns Error when wrong parameters is passed
    Then add a Processor to the Bridge "mybridge" with body is failing with HTTP response code 400:
    """
    {
      "name": "processorInvalid",
      "action": {
        "type": "SlackAction",
        "properties": {
            "channel": "test",
            "webhookUrl": "https://example.com"
      }
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

