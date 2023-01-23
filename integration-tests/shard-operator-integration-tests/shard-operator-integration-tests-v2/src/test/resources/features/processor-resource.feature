Feature: ManagedProcessor tests

  Background:
    Given create Namespace

  @ManagedProcessor
  Scenario: ManagedProcessor resource
    When deploy ProcessorResource:
    """
      apiVersion: com.redhat.service.smartevents/v2alpha1
      kind: ManagedProcessor
      metadata:
        name:  "processor-name"
        labels:
          app.kubernetes.io/managed-by: smartevents-fleet-shard-operator
          app.kubernetes.io/created-by: smartevents-fleet-shard-operator
      spec:
        id: c8f87904-7174-11ed-a1eb-0242ac120002
        name: "processor-name"
        flows:
          - from:
              uri: "timer:update"
              parameters:
                period: 5000
              steps:
                - log: "TickTock - timer is active"
    """
    Then the ProcessorResource "processor-name" exists within 3 minute
    And the Deployment "processor-name" is ready within 3 minute
    #And the Service "processor-name" exists within 3 minute
    #And the ProcessorResource "processor-name" is in condition "Ready" within 3 minutes