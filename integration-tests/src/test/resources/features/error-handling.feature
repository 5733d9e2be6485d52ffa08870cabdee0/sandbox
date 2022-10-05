Feature: Error handling tests

  Background:
    Given authenticate against Manager

  @errorhandling
  Scenario: Error handling Webhook is correctly called if an error occurs
    And create a new Bridge with body:
    """
    {
        "name": "ehBridge",
        "cloud_provider": "aws",
        "region": "us-east-1",
        "error_handler": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
            }
        }
    }
    """
    And the Bridge "ehBridge" is existing with status "ready" within 6 minutes
    And the Ingress of Bridge "ehBridge" is available within 2 minutes

    And add a Processor to the Bridge "ehBridge" with body:
    """
    {
        "name": "whProcessor",
        "action": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "https://example.com/dummy-webhook"
            }
        },
        "transformationTemplate": "{\"sender_name\":\"{data.sender.name}\"}"
    }
    """
    And the list of Processor instances of the Bridge "ehBridge" is containing the Processor "whProcessor"
    And the Processor "whProcessor" of the Bridge "ehBridge" is existing with status "ready" within 3 minutes
    And the Processor "whProcessor" of the Bridge "ehBridge" has action of type "webhook_sink_0.1" and parameters:
      | endpoint | https://example.com/dummy-webhook |

    When send a cloud event to the Ingress of the Bridge "ehBridge":
    """
    {
        "specversion": "1.0",
        "type": "myType",
        "source": "mySource",
        "id": "error-handling-test",
        "subject": "mySubject",
        "data": {
            "myMessage": "Hello world!"
        }
    }
    """
    Then Webhook site with id "${env.webhook.site.uuid}" contains request with text ""id":"${cloud-event.error-handling-test.id}","source":"mySource","type":"myType","subject":"mySubject"" within 1 minute


  @errorhandlingupdate
  Scenario: Bridge Error handler is properly updated
    And create a new Bridge with body:
    """
    {
        "name": "ehBridgeUpdate",
        "cloud_provider": "aws",
        "region": "us-east-1",
        "error_handler": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "https://webhook.site/dummy-webhook"
            }
        }
    }
    """
    And the Bridge "ehBridgeUpdate" is existing with status "ready" within 6 minutes
    And the Ingress of Bridge "ehBridgeUpdate" is available within 2 minutes

    When update the Bridge "ehBridgeUpdate" with body:
    """
    {
        "name": "ehBridgeUpdate",
        "cloud_provider": "aws",
        "region": "us-east-1",
        "error_handler": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
            }
        }
    }
    """
    And the Bridge "ehBridgeUpdate" is existing with status "ready" within 6 minutes
    And the Bridge "ehBridgeUpdate" has errorHandler of type "webhook_sink_0.1" and parameters:
      | endpoint | https://webhook.site/${env.webhook.site.uuid} |
    And the Ingress of Bridge "ehBridgeUpdate" is available within 2 minutes

    And add a Processor to the Bridge "ehBridgeUpdate" with body:
    """
    {
        "name": "whProcessor",
        "action": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
            }
        },
        "transformationTemplate": "{\"sender_name\":\"{data.sender.name}\"}"
    }
    """
    And the list of Processor instances of the Bridge "ehBridgeUpdate" is containing the Processor "whProcessor"
    And the Processor "whProcessor" of the Bridge "ehBridgeUpdate" is existing with status "ready" within 3 minutes
    And the Processor "whProcessor" of the Bridge "ehBridgeUpdate" has action of type "webhook_sink_0.1" and parameters:
      | endpoint | https://webhook.site/${env.webhook.site.uuid} |

    When send a cloud event to the Ingress of the Bridge "ehBridgeUpdate":
    """
    {
        "specversion": "1.0",
        "type": "myType",
        "source": "mySource",
        "id": "error-handling-update-test",
        "subject": "mySubject",
        "data": {
            "myMessage": "Hello world!"
        }
    }
    """
    Then Webhook site with id "${env.webhook.site.uuid}" contains request with text ""id":"${cloud-event.error-handling-update-test.id}","source":"mySource","type":"myType","subject":"mySubject"" within 1 minute

  @pollingerrorhandler
  Scenario: Poll based error handling strategy is configured and an endpoint is available to fetch the last 'N' errors sent to the DLQ for the Bridge
    And create a new Bridge with body:
    """
    {
        "name": "pollingEhBridge",
        "cloud_provider": "aws",
        "region": "us-east-1",
        "error_handler": {
            "type": "endpoint"
        }
    }
    """
    And the Bridge "pollingEhBridge" is existing with status "ready" within 6 minutes
    And the Bridge "pollingEhBridge" has a polling error handler endpoint
    And the Ingress of Bridge "pollingEhBridge" is available within 2 minutes

    And add a Processor to the Bridge "pollingEhBridge" with body:
    """
    {
        "name": "whProcessor",
        "action": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "https://example.com/dummy-webhook"
            }
        },
        "transformationTemplate": "{\"sender_name\":\"{data.sender.name}\"}"
    }
    """
    And the list of Processor instances of the Bridge "pollingEhBridge" is containing the Processor "whProcessor"
    And the Processor "whProcessor" of the Bridge "pollingEhBridge" is existing with status "ready" within 3 minutes
    And the Processor "whProcessor" of the Bridge "pollingEhBridge" has action of type "webhook_sink_0.1" and parameters:
      | endpoint | https://example.com/dummy-webhook |

    When send a cloud event to the Ingress of the Bridge "pollingEhBridge":
    """
    {
        "specversion": "1.0",
        "type": "myType",
        "source": "mySource",
        "id": "error-polling-handler-test",
        "subject": "mySubject",
        "data": {
            "myMessage": "Polling error handler message from ${bridge.pollingEhBridge.id} bridge id"
        }
    }
    """
    Then the polling error handler endpoint of the Bridge "pollingEhBridge" contains message "Polling error handler message from ${bridge.pollingEhBridge.id} bridge id" within 1 minute
    And the polling error handler endpoint of the Bridge "pollingEhBridge" has only 1 error message within 1 minute