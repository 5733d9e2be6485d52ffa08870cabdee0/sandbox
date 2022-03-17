Feature: Slack Action tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

  Scenario: Slack Action Processor is created and slack message should be received in the slack channel
    When add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "slackProcessor",
      "action": {
        "type": "Slack",
        "parameters": {
            "channel": "mc",
            "webhookUrl": "${env.slack.webhook.url}"
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
    And the Processor "slackProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And the Processor "slackProcessor" of the Bridge "mybridge" has action of type "Slack"

    And send a cloud event to the Ingress of the Bridge "mybridge" with path "events":
    """
    {
    "specversion": "1.0",
    "type": "Microsoft.Storage.BlobCreated",
    "source": "StorageService",
    "id": "my-id",
    "time": "2019-11-18T15:13:39.4589254Z",
    "subject": "blobServices/default/containers/{storage-container}/blobs/{new-file}",
    "dataschema": "#",
    "data": {
      "name": "Hello world!"
     }
    }
    """

    Then Slack channel contains message with id "{${bridge.mybridge.cloud-event.my-id.id}" within 1 minute

  Scenario: Slack Action Processor creation returns Error when wrong parameters is passed

    When add a Processor to the Bridge "mybridge" with body is failing with HTTP response code 400:
    """
    {
      "name": "processorInvalid1",
      "action": {
        "type": "Slack",
        "properties": {
            "channel": "test",
            "webhookUrl": "https://example.com"
        }
      }
    }
    """

    When add a Processor to the Bridge "mybridge" with body is failing with HTTP response code 400:
  """
    {
      "name": "processorInvalid2",
      "action": {
        "type": "Slack",
        "parameters": {
            "channel": "",
            "webhookUrl": "https://example.com"
        }
      }
    }
    """

    When add a Processor to the Bridge "mybridge" with body is failing with HTTP response code 400:
  """
    {
      "name": "processorInvalid3",
      "action": {
        "type": "Slack",
        "parameters": {
            "channel": "channel",
            "webhookUrl": ""
        }
      }
    }
    """