Feature: Continuous create Bridges performance tests

  @continuously-create-bridges
  Scenario Outline: Continuously create Bridges by <users> users
    When run benchmark with content:
      """text/vnd.yaml
      name: rhose-continuously-create-bridges
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
            # Using 60 minutes for max duration to make sure that all Bridges are created and deleted within the specified duration
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
              # Create a Bridge with random name suffix
              - randomUUID: bridgeNameSuffix
              - httpRequest:
                  endpoint: manager
                  POST: /api/smartevents_mgmt/v1/bridges
                  body: |
                    {
                      "name": "perf-${bridgeNameSuffix}",
                      "cloud_provider": "aws",
                      "region": "us-east-1"
                    }
                  headers:
                    content-type: application/json
                    authorization: Bearer ${accessToken}
                  handler:
                    body:
                      json:
                        query: .id
                        toVar: bridgeId
            - create-poll:
              # Wait until the Bridge is in either in ready or failed state
              - httpRequest:
                  endpoint: manager
                  GET: /api/smartevents_mgmt/v1/bridges/${bridgeId}
                  headers:
                    content-type: application/json
                    authorization: Bearer ${accessToken}
                    cache-control: no-cache
                  handler:
                    body:
                      json:
                        query: .status
                        toVar: bridgeStatus
              - breakSequence:
                  stringCondition:
                    fromVar: bridgeStatus
                    equalTo: ready
                  onBreak:
                    newSequence:
                      sequence: delete
                      forceSameIndex: true
              - fail:
                  stringCondition:
                    fromVar: bridgeStatus
                    equalTo: failed
                  message: "Bridge creation failed"
              - thinkTime:
                  duration: 5s
              - restartSequence
            - delete:
              # Delete Bridge
              - httpRequest:
                  endpoint: manager
                  DELETE: /api/smartevents_mgmt/v1/bridges/${bridgeId}
                  headers:
                    authorization: Bearer ${accessToken}
            - delete-poll:
              # Wait until the Bridge is in either deleted or in failed state
              - httpRequest:
                  endpoint: manager
                  GET: /api/smartevents_mgmt/v1/bridges/${bridgeId}
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
                        toVar: bridgeStatus
              - breakSequence:
                  intCondition:
                    fromVar: reponseStatus
                    equalTo: 404
              - fail:
                  stringCondition:
                    fromVar: bridgeStatus
                    equalTo: failed
                  message: "Bridge deletion failed"
              - thinkTime:
                  duration: 5s
              - restartSequence
      """
    Then the benchmark run "rhose-continuously-create-bridges" was executed successfully
    And store Manager metrics in Horreum test "continuously-create-bridges-<users>-users"

    Examples:
      | users | shared-connections |
      | 1     | 1                  |

  @continuously-create-bridges-with-error-handler
  Scenario Outline: Continuously create Bridges with error handler by <users> users
    When run benchmark with content:
      """text/vnd.yaml
      name: rhose-continuously-create-bridges-with-error-handler
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
            # Using 60 minutes for max duration to make sure that all Bridges are created and deleted within the specified duration
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
              # Create a Bridge with random name suffix
              - randomUUID: bridgeNameSuffix
              - httpRequest:
                  endpoint: manager
                  POST: /api/smartevents_mgmt/v1/bridges
                  body: |
                    {
                      "name": "perf-${bridgeNameSuffix}",
                      "cloud_provider": "aws",
                      "region": "us-east-1",
                      "error_handler": {
                        "type": "webhook_sink_0.1",
                        "parameters": {
                          "endpoint": "https://webhook.site/dummy-uuid"
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
                        toVar: bridgeId
            - create-poll:
              # Wait until the Bridge is in either in ready or failed state
              - httpRequest:
                  endpoint: manager
                  GET: /api/smartevents_mgmt/v1/bridges/${bridgeId}
                  headers:
                    content-type: application/json
                    authorization: Bearer ${accessToken}
                    cache-control: no-cache
                  handler:
                    body:
                      json:
                        query: .status
                        toVar: bridgeStatus
              - breakSequence:
                  stringCondition:
                    fromVar: bridgeStatus
                    equalTo: ready
                  onBreak:
                    newSequence:
                      sequence: delete
                      forceSameIndex: true
              - fail:
                  stringCondition:
                    fromVar: bridgeStatus
                    equalTo: failed
                  message: "Bridge creation failed"
              - thinkTime:
                  duration: 5s
              - restartSequence
            - delete:
              # Delete Bridge
              - httpRequest:
                  endpoint: manager
                  DELETE: /api/smartevents_mgmt/v1/bridges/${bridgeId}
                  headers:
                    authorization: Bearer ${accessToken}
            - delete-poll:
              # Wait until the Bridge is in either deleted or in failed state
              - httpRequest:
                  endpoint: manager
                  GET: /api/smartevents_mgmt/v1/bridges/${bridgeId}
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
                        toVar: bridgeStatus
              - breakSequence:
                  intCondition:
                    fromVar: reponseStatus
                    equalTo: 404
              - fail:
                  stringCondition:
                    fromVar: bridgeStatus
                    equalTo: failed
                  message: "Bridge deletion failed"
              - thinkTime:
                  duration: 5s
              - restartSequence
      """
    Then the benchmark run "rhose-continuously-create-bridges-with-error-handler" was executed successfully
    And store Manager metrics in Horreum test "continuously-create-bridges-with-error-handler-<users>-users"

    Examples:
      | users | shared-connections |
      | 1     | 1                  |
