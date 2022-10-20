Feature: Webhook Action tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge" in cloud provider "aws" and region "us-east-1"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes


  Scenario: Webhook is correctly called

    Given add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "type": "webhook_sink_0.1",
        "parameters": {
            "endpoint": "https://webhook.site/${webhook.site.token.first}"
        }
      },
      "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
    }
    """
    And the list of Processor instances of the Bridge "mybridge" is containing the Processor "myProcessor"
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And the Processor "myProcessor" of the Bridge "mybridge" has action of type "webhook_sink_0.1" and parameters:
      | endpoint | https://webhook.site/${webhook.site.token.first} |

    When send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
      "specversion": "1.0",
      "type": "webhook.site.invoked",
      "source": "WebhookActionTestService",
      "id": "webhook-test",
      "data": {
          "name": "world"
        }
    }
    """
    Then Webhook site with id "${webhook.site.token.first}" contains request with text "hello world by ${cloud-event.webhook-test.id}" within 1 minute


  Scenario: Webhook Processor is correctly updated

    Given add a Processor to the Bridge "mybridge" with body:
      """
      {
        "name": "testProcessor",
        "action": {
          "type": "webhook_sink_0.1",
          "parameters": {
              "endpoint": "https://webhook.site/${webhook.site.token.first}"
          }
        },
        "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
      }
      """
    And the list of Processor instances of the Bridge "mybridge" is containing the Processor "testProcessor"
    And the Processor "testProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes

    When update the Processor "testProcessor" of the Bridge "mybridge" with body:
      """
      {
        "name": "testProcessor",
        "action": {
          "type": "webhook_sink_0.1",
          "parameters": {
              "endpoint": "https://webhook.site/${webhook.site.token.second}"
          }
        },
        "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
      }
      """

    And the Processor "testProcessor" of the Bridge "mybridge" has action of type "webhook_sink_0.1" and parameters:
      | endpoint | https://webhook.site/${webhook.site.token.second} |
    And the list of Processor instances of the Bridge "mybridge" is containing the Processor "testProcessor"
    And the Processor "testProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And wait for 10 seconds

    When send a cloud event to the Ingress of the Bridge "mybridge":
      """
      {
        "specversion": "1.0",
        "type": "webhook.site.invoked",
        "source": "WebhookActionTestService",
        "id": "webhook-test-update",
        "data": {
            "name": "world"
          }
      }
      """

    Then Webhook site with id "${webhook.site.token.second}" contains request with text "hello world by ${cloud-event.webhook-test-update.id}" within 1 minute
