Feature: Slack Action tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge" in cloud provider "aws" and region "us-east-1"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

  Scenario: Slack Action Processor is created and slack message should be received in the slack channel
    When add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "slackProcessor",
      "action": {
        "type": "slack_sink_0.1",
        "parameters": {
            "slack_channel": "${slack.channel.mc.name}",
            "slack_webhook_url": "${slack.channel.mc.webhook.url}"
          }
      }
    }
    """
    And the Processor "slackProcessor" of the Bridge "mybridge" is existing with status "ready" within 5 minutes
    And the Processor "slackProcessor" of the Bridge "mybridge" has action of type "slack_sink_0.1"

    And send a cloud event to the Ingress of the Bridge "mybridge":
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

    Then Slack channel "${slack.channel.mc.name}" contains message with text "${cloud-event.my-id.id}" within 1 minute


  Scenario: Slack Action Processor is created and slack webhook url getting updated and slack message should be received in the new slack channel
    When add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "slackProcessor",
      "action": {
        "type": "slack_sink_0.1",
        "parameters": {
            "slack_channel": "${slack.channel.general.name}",
            "slack_webhook_url": "${slack.channel.general.webhook.url}"
          }
      }
    }
    """
    And the Processor "slackProcessor" of the Bridge "mybridge" is existing with status "ready" within 5 minutes
    And the Processor "slackProcessor" of the Bridge "mybridge" has action of type "slack_sink_0.1"

    And send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
      "specversion": "1.0",
      "type": "Microsoft.Storage.BlobCreated",
      "source": "StorageService",
      "id": "test-id",
      "time": "2019-11-18T15:13:39.4589254Z",
      "subject": "blobServices/default/containers/{storage-container}/blobs/{new-file}",
      "dataschema": "#",
      "data": {
        "name": "Hello world!"
        }
    }
    """
    Then Slack channel "${slack.channel.general.name}" contains message with text "${cloud-event.test-id.id}" within 1 minute

    When update the Processor "slackProcessor" of the Bridge "mybridge" with body:
    """
    {
      "name": "slackProcessor",
      "action": {
        "type": "slack_sink_0.1",
        "parameters": {
            "slack_channel": "${slack.channel.mc.name}",
            "slack_webhook_url": "${slack.channel.mc.webhook.url}"
          }
      }
    }
    """
    And the Processor "slackProcessor" of the Bridge "mybridge" is existing with status "ready" within 5 minutes

    And send a cloud event to the Ingress of the Bridge "mybridge":
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

    Then Slack channel "${slack.channel.mc.name}" contains message with text "${cloud-event.my-id.id}" within 1 minute