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
            "channel": "${env.slack.channel.name}",
            "webhookUrl": "${env.slack.webhook.url}"
            }
      }
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

    Then Slack channel contains message with text "${bridge.mybridge.cloud-event.my-id.id}" within 1 minute
