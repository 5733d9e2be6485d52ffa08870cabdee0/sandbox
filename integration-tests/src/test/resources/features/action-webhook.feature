Feature: Webhook Action tests

  @webhookaction
  Scenario: Webhook is correctly called
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "type": "Webhook",
        "parameters": {
            "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
        }
      },
      "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
    }
    """
    And the list of Processor instances of the Bridge "mybridge" is containing the Processor "myProcessor"
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And the Processor "myProcessor" of the Bridge "mybridge" has action of type "Webhook" and parameters:
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
    Then Webhook site contains request with text "hello world by ${bridge.mybridge.cloud-event.webhook-test.id}" within 1 minute
