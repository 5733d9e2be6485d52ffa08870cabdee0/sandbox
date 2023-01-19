Feature: ManagedProcessor tests

  Background:
    Given create Namespace

  @VIP
  Scenario: ManagedProcessor resource
    When deploy ProcessorResource:
    """
      apiVersion:  com.redhat.service.smartevents/v2alpha1
      kind: ManagedProcessor
      metadata:
        name: "processor-name"
      spec:
        name: "processor-name"
        flows:
          - from:
              uri: "rhose:bridge"
              steps:
              - to:
                  uri: "sink-name"
    """
    Then the ProcessorResource "processor-name" exists within 3 minute
    And the Deployment "processor-name" is ready within 3 minute
    And the Service "processor-name" exists within 3 minute
    And the BridgeExecutor "processor-name" is in condition "Ready" within 3 minutes