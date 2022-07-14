Feature: Periodic Slack Action tests

  Scenario: Slack message sent to existing Ingress endpoint should be received in the slack channel
    Given authenticate against Manager

    When send a cloud event to the endpoint URL "${env.remote.cluster.bridge.endpoint}":
    """
    {
      "specversion": "1.0",
      "type": "continuous.ci",
      "source": "SlackService",
      "id": "my-id",
      "data": {
        "name": "Hello world by dev continuous CI!"
        }
    }
    """

    Then Slack channel "${slack.channel.mc.name}" contains message with text "${cloud-event.my-id.id}" within 1 minute
