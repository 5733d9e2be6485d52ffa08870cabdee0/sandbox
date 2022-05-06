Feature: Slack Source tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

  @slacksource
  Scenario: Slack Source Processor is created and slack message as source should result into other generated slack message
    When add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "slackSourceProcessor",
      "source": {
        "type": "Slack",
        "parameters": {
            "channel": "${env.slack.channel.name}",
            "token": "${env.slack.webhook.token}"
          }
      },
      "filters": [
        {
          "key": "data.text",
          "type": "StringBeginsWith",
          "values": ["Slack Event Source Feature trigger"]
        }
      ]
    }
    """
    And the Processor "slackSourceProcessor" of the Bridge "mybridge" is existing with status "ready" within 5 minutes
    And the Processor "slackSourceProcessor" of the Bridge "mybridge" has source of type "Slack"

    When add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "slackForwardProcessor",
      "action": {
        "type": "Slack",
        "parameters": {
            "channel": "${env.slack.channel.name}",
            "webhookUrl": "${env.slack.webhook.url}"
          }
      },
      "transformationTemplate": "Message {data.text} was observed"
    }
    """
    And the Processor "slackForwardProcessor" of the Bridge "mybridge" is existing with status "ready" within 5 minutes
    And the Processor "slackForwardProcessor" of the Bridge "mybridge" has action of type "Slack"


    And create message with text "Slack Event Source Feature trigger ${uuid.slack.source.trigger}" on slack channel

    Then Slack channel contains message with text "Message Slack Event Source Feature trigger ${uuid.slack.source.trigger} was observed" within 1 minute

