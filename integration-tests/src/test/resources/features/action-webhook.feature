Feature: Webhook Action tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

  @webhookaction
  Scenario: Webhook is correctly called

    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "type": "webhook_sink_0.1",
        "parameters": {
            "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
        }
      },
      "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
    }
    """
    And the list of Processor instances of the Bridge "mybridge" is containing the Processor "myProcessor"
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And the Processor "myProcessor" of the Bridge "mybridge" has action of type "webhook_sink_0.1" and parameters:
      | endpoint | https://webhook.site/${env.webhook.site.uuid} |

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
    Then Webhook site contains request with text "hello world by ${cloud-event.webhook-test.id}" within 1 minute


    Scenario: Webhook Processor is correctly updated

      Given add a Processor to the Bridge "testbridge" with body:
      """
      {
        "name": "testProcessor",
        "action": {
          "type": "webhook_sink_0.1",
          "parameters": {
              "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
          }
        },
        "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
      }
      """
      And the list of Processor instances of the Bridge "testbridge" is containing the Processor "testProcessor"
      And the Processor "testProcessor" of the Bridge "testbridge" is existing with status "ready" within 3 minutes

      When update the Processor "testProcessor" of the Bridge "testbridge" with body:
      """
      {
        "name": "testProcessor",
        "action": {
          "type": "webhook_sink_0.1",
          "parameters": {
              "endpoint": "https://webhook.site/${env.webhook.site.uuid.second}"
          }
        },
        "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
      }
      """

      And the Processor "testProcessor" of the Bridge "testbridge" has action of type "webhook_sink_0.1" and parameters:
        | endpoint | https://webhook.site/${env.webhook.site.uuid.second}|
      And the list of Processor instances of the Bridge "testbridge" is containing the Processor "testProcessor"
      And the Processor "testProcessor" of the Bridge "testbridge" is existing with status "ready" within 3 minutes
      And wait for 10 seconds

      When send a cloud event to the Ingress of the Bridge "testbridge":
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
      Then Webhook site contains updated request with text "hello world by ${cloud-event.webhook-test-update.id}" within 1 minute


