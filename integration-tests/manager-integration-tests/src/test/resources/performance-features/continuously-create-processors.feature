Feature: Continuous create Processors performance tests

  Background:
    Given authenticate against Manager
    And create a new Bridge "my-perf-bridge" in cloud provider "aws" and region "us-east-1"
    And the Bridge "my-perf-bridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "my-perf-bridge" is available within 2 minutes

  @continuously-create-processors
  Scenario Outline: Continuously create Processors by <users> users
    When run benchmark with content:
      """text/vnd.yaml
      name: rhose-continuously-create-processors
      http:
      - host: ${env.event-bridge.manager.url}
        name: manager
        sharedConnections: <shared-connections>
        connectionStrategy: ALWAYS_NEW
      - host: https://sso.redhat.com
        name: sso
      phases:
      - bridgeCreatingUser:
          always:
            # Using 60 minutes for max duration to make sure that all Processors are created and deleted within the specified duration
            maxDuration: 60m
            users: <users>
            maxIterations: 10
            scenario:
            - authenticate:
              - set: clientId <- ${env.bridge.client.id}
              - set: clientSecret <- ${env.bridge.client.secret}
              - httpRequest:
                  endpoint: sso
                  POST: /auth/realms/redhat-external/protocol/openid-connect/token
                  body: client_id=${clientId}&grant_type=client_credentials&client_secret=${clientSecret}
                  headers:
                    content-type: application/x-www-form-urlencoded
                  handler:
                    body:
                      json:
                        query: .access_token
                        toVar: accessToken
            - create:
              # Create a Processor with random name suffix
              - set: bridgeId <- ${bridge.my-perf-bridge.id}
              - randomUUID: processorNameSuffix
              - httpRequest:
                  endpoint: manager
                  POST: /api/smartevents_mgmt/v1/bridges/${bridgeId}/processors
                  body: |
                    {
                      "name": "perf-${processorNameSuffix}",
                      "action": {
                        "type": "webhook_sink_0.1",
                        "parameters": {
                          "endpoint": "https://example.com/my-webhook-endpoint"
                        }
                      }
                    }
                  headers:
                    content-type: application/json
                    authorization: Bearer ${accessToken}
                  handler:
                    body:
                      json:
                        query: .id
                        toVar: processorId
            - create-poll:
              # Wait until the Processor is in either in ready or failed state
              - httpRequest:
                  endpoint: manager
                  GET: /api/smartevents_mgmt/v1/bridges/${bridgeId}/processors/${processorId}
                  headers:
                    content-type: application/json
                    authorization: Bearer ${accessToken}
                    cache-control: no-cache
                  handler:
                    body:
                      json:
                        query: .status
                        toVar: processorStatus
              - breakSequence:
                  stringCondition:
                    fromVar: processorStatus
                    equalTo: ready
                  onBreak:
                    newSequence:
                      sequence: delete
                      forceSameIndex: true
              - fail:
                  stringCondition:
                    fromVar: processorStatus
                    equalTo: failed
                  message: "Processor creation failed"
              - thinkTime:
                  duration: 5s
              - restartSequence
            - delete:
              # Delete Processor
              - httpRequest:
                  endpoint: manager
                  DELETE: /api/smartevents_mgmt/v1/bridges/${bridgeId}/processors/${processorId}
                  headers:
                    authorization: Bearer ${accessToken}
            - delete-poll:
              # Wait until the Processor is in either deleted or in failed state
              - httpRequest:
                  endpoint: manager
                  GET: /api/smartevents_mgmt/v1/bridges/${bridgeId}/processors/${processorId}
                  headers:
                    content-type: application/json
                    authorization: Bearer ${accessToken}
                    cache-control: no-cache
                  handler:
                    status:
                      store:
                        toVar: reponseStatus
                    body:
                      json:
                        query: .status
                        toVar: processorStatus
              - breakSequence:
                  intCondition:
                    fromVar: reponseStatus
                    equalTo: 404
              - fail:
                  stringCondition:
                    fromVar: processorStatus
                    equalTo: failed
                  message: "Processor deletion failed"
              - thinkTime:
                  duration: 5s
              - restartSequence
      """

    Then the benchmark run "rhose-continuously-create-processors" was executed successfully
    And store Manager metrics in Horreum test "continuously-create-processors-<users>-users"

    Examples:
      | users | shared-connections |
      | 1     | 1                  |
