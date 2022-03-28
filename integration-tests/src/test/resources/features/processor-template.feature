Feature: Tests of Processor Transformation template

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

  Scenario: Transform cloud event to Slack message and send it using WebHook
    When add a Processor to the Bridge "mybridge" with body:
    """
    {
     "name": "myProcessor",
      "action": {
        "parameters": {
            "endpoint": "${env.slack.webhook.url}"
       },
        "type": "Webhook"
      },
      "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
    "specversion": "1.0",
    "type": "hello.invoked",
    "source": "HelloService",
    "id": "my-id",
    "data": {
        "name": "world"
      }
    }
    """
    
    Then Slack channel contains message with text "hello world by ${bridge.mybridge.cloud-event.my-id.id}" within 1 minute
