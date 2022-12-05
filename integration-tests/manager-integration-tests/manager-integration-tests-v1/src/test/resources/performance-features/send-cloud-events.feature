Feature: Sending cloud events performance tests

  Background:
    Given authenticate against Manager

  @send-cloud-events-single-bridge
  Scenario Outline: Creating one single bridge and sending cloud events of size <messageContentSizeInBytes> with <requestsPerSec> requests per second
    And create a new Bridge "my-perf-single-bridge" in cloud provider "aws" and region "us-east-1"
    And the Bridge "my-perf-single-bridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "my-perf-single-bridge" is available within 2 minutes
    And add a Processor to the Bridge "my-perf-single-bridge" with body:
    """
    {
        "name": "my-perf-processor",
        "action": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "${env.performance.webhook.url}"
            }
        },
        "transformationTemplate" : "{\"bridgeId\": \"{data.bridgeId}\", \"message\": \"{data.message}\", \"submitted_at\": \"{timestamp}\"}"
    }
    """
    And the Processor "my-perf-processor" of the Bridge "my-perf-single-bridge" is existing with status "ready" within 3 minutes

    When generate <messageContentSizeInBytes> random letters into data property "random-data"
    And run benchmark with content:
      """text/vnd.yaml
      name: rhose-send-cloud-events-single-bridge
      agents:
        driver01:
#          log: my-config-map/log4j2.xml
      http:
      - host: ${bridge.my-perf-single-bridge.endpoint.base}
        sharedConnections: 200
        connectionStrategy: ALWAYS_NEW
      phases:
      - steadyState:
          constantRate:
            usersPerSec: <requestsPerSec>
            maxSessions: 200
            startAfter: rampUp
            duration: 5m
            maxDuration: 6m
            scenario:
            - send-cloud-event:
              - timestamp:
                  toVar: submitted_at
                  pattern: "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
              - httpRequest:
                  POST: ${bridge.my-perf-single-bridge.endpoint.path}
                  body: |
                    {
                      "specversion": "1.0",
                      "type": "webhook.site.invoked",
                      "source": "WebhookActionTestService",
                      "id": "webhook-perf-test",
                      "timestamp": "${submitted_at}",
                      "data": {
                          "bridgeId": "${bridge.my-perf-single-bridge.id}",
                          "message": "${data.random-data}"
                        }
                    }
                  headers:
                    content-type: application/cloudevents+json
                    authorization: Bearer ${manager.authentication.token}
      - rampUp:
          increasingRate:
            initialUsersPerSec: 1
            targetUsersPerSec: 10
            maxSessions: 20
            duration: 1m
            isWarmup: true
            scenario:
            - send-cloud-event:
              - timestamp:
                  toVar: submitted_at
                  pattern: "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
              - httpRequest:
                  POST: ${bridge.my-perf-single-bridge.endpoint.path}
                  body: |
                    {
                      "specversion": "1.0",
                      "type": "webhook.site.invoked",
                      "source": "WebhookActionTestService",
                      "id": "webhook-test",
                      "timestamp": "${submitted_at}",
                      "data": {
                          "bridgeId": "",
                          "message": "hello bridge"
                        }
                    }
                  headers:
                    content-type: application/cloudevents+json
                    authorization: Bearer ${manager.authentication.token}
      """
    And the benchmark run "rhose-send-cloud-events-single-bridge" was executed successfully
    And the total of events received for benchmark "rhose-send-cloud-events-single-bridge" run in "steadyState" phase is equal to the total of cloud events sent in:
      | bridge                | metric           |
      | my-perf-single-bridge | send-cloud-event |
    And store generated report of benchmark run "rhose-send-cloud-events-single-bridge" to file "send-cloud-events-single-bridge-<messageContentSizeInBytes>-message-size-<requestsPerSec>-users.html"
    And store results of benchmark run "rhose-send-cloud-events-single-bridge" to json file "send-cloud-events-single-bridge-<messageContentSizeInBytes>-message-size-<requestsPerSec>-users.json"

    Examples:
      | requestsPerSec | messageContentSizeInBytes |
      | 10             | 10                        |
      | 50             | 10                        |
      | 10             | 5000                      |
      | 50             | 5000                      |

  @send-cloud-events-two-bridges
  Scenario Outline: Creating two bridges and sending cloud events with <requestsPerSec> requests per second
    Given create a new Bridge "my-perf-bridge-1" in cloud provider "aws" and region "us-east-1"
    And create a new Bridge "my-perf-bridge-2" in cloud provider "aws" and region "us-east-1"
    And the Bridge "my-perf-bridge-1" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "my-perf-bridge-1" is available within 2 minutes
    And the Bridge "my-perf-bridge-2" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "my-perf-bridge-2" is available within 2 minutes

    And add a Processor to the Bridge "my-perf-bridge-1" with body:
    """
    {
        "name": "my-perf-processor-1",
        "action": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "${env.performance.webhook.url}"
            }
        },
        "transformationTemplate" : "{\"bridgeId\": \"{data.bridgeId}\", \"message\": \"{data.message}\", \"submitted_at\": \"{timestamp}\"}"
    }
    """
    And add a Processor to the Bridge "my-perf-bridge-2" with body:
    """
    {
        "name": "my-perf-processor-2",
        "action": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "${env.performance.webhook.url}"
            }
        },
        "transformationTemplate" : "{\"bridgeId\": \"{data.bridgeId}\", \"message\": \"{data.message}\", \"submitted_at\": \"{timestamp}\"}"
    }
    """
    And the Processor "my-perf-processor-1" of the Bridge "my-perf-bridge-1" is existing with status "ready" within 3 minutes
    And the Processor "my-perf-processor-2" of the Bridge "my-perf-bridge-2" is existing with status "ready" within 3 minutes

    When generate <messageContentSizeInBytes> random letters into data property "random-data"
    And run benchmark with content:
      """text/vnd.yaml
      name: rhose-send-cloud-events-several-bridges
      agents:
        driver01:
      http:
      - host: ${bridge.my-perf-bridge-1.endpoint.base}
        name: bridge-1
        sharedConnections: 200
        connectionStrategy: ALWAYS_NEW
      - host: ${bridge.my-perf-bridge-2.endpoint.base}
        name: bridge-2
        sharedConnections: 200
        connectionStrategy: ALWAYS_NEW
      phases:
      - steadyStateBridge1:
          constantRate:
            usersPerSec: <requestsPerSec>
            maxSessions: 200
            startAfter: rampUp
            duration: 5m
            maxDuration: 6m
            scenario:
            - send-cloud-event-bridge-1:
              - timestamp:
                  toVar: submitted_at
                  pattern: "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
              - httpRequest:
                  authority: ${bridge.my-perf-bridge-1.endpoint.authority}
                  POST: ${bridge.my-perf-bridge-1.endpoint.path}
                  body: |
                    {
                      "specversion": "1.0",
                      "type": "webhook.site.invoked",
                      "source": "WebhookActionTestService",
                      "id": "webhook-perf-test",
                      "timestamp": "${submitted_at}",
                      "data": {
                          "bridgeId": "${bridge.my-perf-bridge-1.id}",
                          "message": "${data.random-data}"
                        }
                    }
                  headers:
                    content-type: application/cloudevents+json
                    authorization: Bearer ${manager.authentication.token}
      - steadyStateBridge2:
          constantRate:
            usersPerSec: <requestsPerSec>
            maxSessions: 200
            startAfter: rampUp
            duration: 5m
            maxDuration: 6m
            scenario:
            - send-cloud-event-bridge-2:
              - timestamp:
                  toVar: submitted_at
                  pattern: "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
              - httpRequest:
                  authority: ${bridge.my-perf-bridge-2.endpoint.authority}
                  POST: ${bridge.my-perf-bridge-2.endpoint.path}
                  body: |
                    {
                      "specversion": "1.0",
                      "type": "webhook.site.invoked",
                      "source": "WebhookActionTestService",
                      "id": "webhook-perf-test",
                      "timestamp": "${submitted_at}",
                      "data": {
                          "bridgeId": "${bridge.my-perf-bridge-2.id}",
                          "message": "${data.random-data}"
                        }
                    }
                  headers:
                    content-type: application/cloudevents+json
                    authorization: Bearer ${manager.authentication.token}
      - rampUp:
          increasingRate:
            initialUsersPerSec: 1
            targetUsersPerSec: 10
            maxSessions: 20
            duration: 1m
            isWarmup: true
            scenario:
            - send-cloud-event-bridge-1:
              - timestamp:
                  toVar: submitted_at
                  pattern: "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
              - httpRequest:
                  authority: ${bridge.my-perf-bridge-1.endpoint.authority}
                  POST: ${bridge.my-perf-bridge-1.endpoint.path}
                  body: |
                    {
                      "specversion": "1.0",
                      "type": "webhook.site.invoked",
                      "source": "WebhookActionTestService",
                      "id": "webhook-test",
                      "timestamp": "${submitted_at}",
                      "data": {
                          "bridgeId": "",
                          "message": "hello bridge-1"
                        }
                    }
                  headers:
                    content-type: application/cloudevents+json
                    authorization: Bearer ${manager.authentication.token}
            - send-cloud-event-bridge-2:
              - timestamp:
                  toVar: submitted_at
                  pattern: "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
              - httpRequest:
                  authority: ${bridge.my-perf-bridge-2.endpoint.authority}
                  POST: ${bridge.my-perf-bridge-2.endpoint.path}
                  body: |
                    {
                      "specversion": "1.0",
                      "type": "webhook.site.invoked",
                      "source": "WebhookActionTestService",
                      "id": "webhook-test",
                      "timestamp": "${submitted_at}",
                      "data": {
                          "bridgeId": "",
                          "message": "hello bridge-2"
                        }
                    }
                  headers:
                    content-type: application/cloudevents+json
                    authorization: Bearer ${manager.authentication.token}
      """
    And the benchmark run "rhose-send-cloud-events-several-bridges" was executed successfully
    And the total of events received for benchmark "rhose-send-cloud-events-several-bridges" run in "steadyStateBridge1" phase is equal to the total of cloud events sent in:
      | bridge           | metric                    |
      | my-perf-bridge-1 | send-cloud-event-bridge-1 |
    And the total of events received for benchmark "rhose-send-cloud-events-several-bridges" run in "steadyStateBridge2" phase is equal to the total of cloud events sent in:
      | bridge           | metric                    |
      | my-perf-bridge-2 | send-cloud-event-bridge-2 |
    And store generated report of benchmark run "rhose-send-cloud-events-several-bridges" to file "send-cloud-events-several-bridges-<messageContentSizeInBytes>-message-size-<requestsPerSec>-users.html"
    And store results of benchmark run "rhose-send-cloud-events-several-bridges" to json file "send-cloud-events-several-bridges-<messageContentSizeInBytes>-message-size-<requestsPerSec>-users.json"

    Examples:
      | requestsPerSec | messageContentSizeInBytes |
      | 10             | 10                        |
      | 50             | 10                        |
      | 10             | 5000                      |
      | 50             | 5000                      |
