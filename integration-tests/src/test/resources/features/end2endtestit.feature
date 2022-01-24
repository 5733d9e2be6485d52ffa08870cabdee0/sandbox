Feature: End to End Bridge integration tests

  Scenario:By default Manager url should not be accessible without authentication
    Given Manager url is not accessible

  Scenario: Bridge is created and in available state
    Given Bridge list is empty
    When Create a Bridge
    Then Bridge is exists in status "AVAILABLE" within 2 minute

  Scenario: Processor gets created to the bridge and deployed
    When Add processor to the bridge
    Then Add wrong filter processor
    And Processor is exists in status "AVAILABLE" within 3 minutes

    And Ingress endpoint is accessible

  Scenario: Bridge, Processor and Ingress gets deleted
    When Processor is deleted
    Then Processor doesn't exists within 2 minutes

    When Delete a Bridge
    Then Bridge doesn't exists within 2 minutes

    And Ingress is Undeployed within 1 minute

  Scenario: Verify Metrics details exist
    Given Metrics info is exists