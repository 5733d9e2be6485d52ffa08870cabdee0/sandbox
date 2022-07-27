Feature: Create Bridges performance tests

  Background:
    Given authenticate against Manager

  @create-bridges-at-once
  Scenario Outline: Create and delete many Bridges at once
    When Create benchmark on Hyperfoil "hf-controller" instance with content:
      """text/vnd.yaml
      name: rhose-create-bridges
      http:
      - host: ${env.event-bridge.manager.url}
        sharedConnections: <shared-connections>
        connectionStrategy: ALWAYS_NEW
      phases:
      - bridgeCreatingUser:
          atOnce:
            # Using 15 minutes for max duration to make sure that all Bridges are created and deleted within the specified duration
            maxDuration: 15m
            users: <bridges>
            scenario:
            - create:
              # Create a Bridge with random name suffix
              - randomUUID: bridgeNameSuffix
              - httpRequest:
                  POST: /api/smartevents_mgmt/v1/bridges
                  body: |
                    {
                      "name": "perf-${bridgeNameSuffix}"
                    }
                  headers:
                    content-type: application/json
                    authorization: Bearer ${manager.authentication.token}
                  handler:
                    body:
                      json:
                        query: .id
                        toVar: bridgeId
            - create-poll:
              # Wait until the Bridge is in either in ready or failed state
              - httpRequest:
                  GET: /api/smartevents_mgmt/v1/bridges/${bridgeId}
                  headers:
                    content-type: application/json
                    authorization: Bearer ${manager.authentication.token}
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
                  DELETE: /api/smartevents_mgmt/v1/bridges/${bridgeId}
                  headers:
                    authorization: Bearer ${manager.authentication.token}
            - delete-poll:
              # Wait until the Bridge is in either deleted or in failed state
              - httpRequest:
                  GET: /api/smartevents_mgmt/v1/bridges/${bridgeId}
                  headers:
                    content-type: application/json
                    authorization: Bearer ${manager.authentication.token}
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
    Then Run benchmark "rhose-create-bridges" on Hyperfoil "hf-controller" instance within 15 minutes

    Examples:
      | bridges | shared-connections |
      | 20      | 20                 |
