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
        name: "processor-name-metadata"
      spec:
        name: "processor-name"
        flows:
          - from:
              uri: "rhose:bridge"
              steps:
              - to:
                  uri: "sink-name"
    """
    Then the ProcessorResource "processor-name" exists within 1 minute