@test
Feature: Processor tests

  @test2
  Scenario: Processor is created, deployed and correctly deleted
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "parameters": {
            "topic":  "myKafkaTopic"
        },
        "type": "KafkaTopic"
      }
    }
    """
    And the list of Processor instances of the Bridge "mybridge" is containing the Processor "myProcessor"
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And the Processor "myProcessor" of the Bridge "mybridge" has action of type "KafkaTopic" and parameters:
      | topic | myKafkaTopic |

    When delete the Processor "myProcessor" of the Bridge "mybridge"
    
    Then the Processor "myProcessor" of the Bridge "mybridge" is not existing within 2 minutes

    And the Manager metric 'manager_processor_status_change_total{status="PROVISIONING",}' count is at least 1
    And the Manager metric 'manager_processor_status_change_total{status="READY",}' count is at least 1
    And the Manager metric 'manager_processor_status_change_total{status="DELETED",}' count is at least 1

  Scenario: Add a Processor with no action
    Given authenticate against Manager
    
    When create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes
    
    Then add a Processor to the Bridge "mybridge" with body is failing with HTTP response code 400:
    """
    {
      "name": "noActionProcessor"
    }
    """

  Scenario: Processor payload is malformed
    Given authenticate against Manager
    
    When create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes
    
    Then add a Processor to the Bridge "mybridge" with body is failing with HTTP response code 400:
    """
    {
      "name": "processorInvalid"
      "filters": []
    }
    """

  Scenario: Cannot access the list of Processors without authentication
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

    When logout of Manager
    
    Then the list of Processor instances of the Bridge "mybridge" is failing with HTTP response code 401

  Scenario: Cannot create a processor without authentication
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

    When logout of Manager
    
    Then add a Processor to the Bridge "mybridge" with body is failing with HTTP response code 401:
    """
    {
      "name": "myProcessor",
      "action": {
        "parameters": {
            "topic":  "myKafkaTopic"
        },
        "type": "KafkaTopic"
      }
    }
    """

  Scenario: Cannot access processor details without authentication
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 2 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "parameters": {
            "topic":  "myKafkaTopic"
        },
        "type": "KafkaTopic"
      }
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes

    When logout of Manager

    Then get Processor "myProcessor" of the Bridge "mybridge" is failing with HTTP response code 401