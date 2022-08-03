Feature: Sending cloud events performance tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "my-perf-bridge" in cloud provider "aws" and region "us-east-1"
    And the Bridge "my-perf-bridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "my-perf-bridge" is available within 2 minutes
    And add a Processor to the Bridge "my-perf-bridge" with body:
    """
    {
        "name": "my-perf-processor",
        "action": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "${env.performance.slack.webhook.url}"
            }
        },
        "transformationTemplate" : "{\"bridgeId\": \"{data.bridgeId}\", \"message\": \"{data.message}\"}"
    }
    """
    And the Processor "my-perf-processor" of the Bridge "my-perf-bridge" is existing with status "ready" within 3 minutes

  @performance
  Scenario Outline: Sending Cloud Event scenario with usersPerSec <usersPerSec>
    When Create benchmark on Hyperfoil "hf-controller" instance with content:
      """text/vnd.yaml
      name: rhose-send-cloud-events
      agents:
      - agent-one
      - agent-two
      http:
      - host: ${bridge.my-perf-bridge.endpoint.base}
        sharedConnections: 5
        connectionStrategy: ALWAYS_NEW
      phases:
      - steadyState:
          constantRate:
            usersPerSec: <usersPerSec>
            maxSessions: 10
            startAfter: rampUp
            duration: 5m
            maxDuration: 6m
            scenario:
            - send-cloud-event:
              - httpRequest:
                  POST: ${bridge.my-perf-bridge.endpoint.path}
                  body: |
                    {
                      "specversion": "1.0",
                      "type": "webhook.site.invoked",
                      "source": "WebhookActionTestService",
                      "id": "webhook-test",
                      "data": {
                          "bridgeId": "${bridge.my-perf-bridge.id}",
                          "message": "hello bridge"
                        }
                    }
                  headers:
                    content-type: application/cloudevents+json
                    authorization: Bearer ${manager.authentication.token}
      - rampUp:
          increasingRate:
            initialUsersPerSec: 1
            targetUsersPerSec: 10
            maxSessions: 10
            duration: 1m
            isWarmup: true
            scenario:
            - send-cloud-event:
              - httpRequest:
                  POST: ${bridge.my-perf-bridge.endpoint.path}
                  body: |
                    {
                      "specversion": "1.0",
                      "type": "webhook.site.invoked",
                      "source": "WebhookActionTestService",
                      "id": "webhook-test",
                      "data": {
                          "bridgeId": "${bridge.my-perf-bridge.id}",
                          "message": "hello bridge"
                        }
                    }
                  headers:
                    content-type: application/cloudevents+json
                    authorization: Bearer ${manager.authentication.token}
      """
    Then Run benchmark "rhose-send-cloud-events" on Hyperfoil "hf-controller" instance within 15 minutes
    And number of cloud events sent is greater than 0

    Examples:
      | usersPerSec |
      | 1         |
      | 4         |
      | 8         |