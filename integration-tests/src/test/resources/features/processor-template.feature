Feature: Tests of Processor Transformation template

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 5 minutes
    And the Ingress of Bridge "mybridge" is available within 3 minutes


  Scenario: Transform cloud event to Slack message and send it using WebHook
    When add a Processor to the Bridge "mybridge" with body:
    """
    {
     "name": "myProcessor",
      "action": {
        "parameters": {
            "endpoint": "${env.slack.webhook.url}"
       },
        "type": "webhook_sink_0.1"
      },
      "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 5 minutes
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

    Then Slack channel contains message with text "hello world by ${cloud-event.my-id.id}" within 1 minute


  Scenario: Transformation template is properly updated
    Given add a Processor to the Bridge "mybridge" with body:
    """
    {
     "name": "myProcessor",
      "action": {
        "parameters": {
            "endpoint": "${env.slack.webhook.url}"
       },
        "type": "webhook_sink_0.1"
      },
      "transformationTemplate" : "{\"text\": \"hello {data.name} by {id}\"}"
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 5 minutes
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
    And Slack channel contains message with text "hello world by ${cloud-event.my-id.id}" within 1 minute

    When update the Processor "myProcessor" of the Bridge "mybridge" with body:
    """
    {
     "name": "myProcessor",
      "action": {
        "parameters": {
            "endpoint": "${env.slack.webhook.url}"
       },
        "type": "webhook_sink_0.1"
      },
      "transformationTemplate" : "{\"text\": \"hello {data.name} by updated template {id}\"}"
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 5 minutes
    # Need to wait until original Processor pod is completely terminated, see https://issues.redhat.com/browse/MGDOBR-613
    And wait for 10 seconds
    And send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
    "specversion": "1.0",
    "type": "hello.invoked",
    "source": "HelloService",
    "id": "second-event-id",
    "data": {
        "name": "world"
      }
    }
    """

    Then Slack channel contains message with text "hello world by updated template ${cloud-event.second-event-id.id}" within 1 minute
