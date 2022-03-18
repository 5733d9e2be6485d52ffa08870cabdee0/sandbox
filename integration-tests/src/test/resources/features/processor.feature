Feature: Processor tests

  Scenario: Processor is created, deployed and correctly deleted
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
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
