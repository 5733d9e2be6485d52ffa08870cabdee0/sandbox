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

    And the Manager metric 'managed_services_api_rhose_operation_success_count_total{operation="provision",resource="bridge",}' count is at least 1
    And the Manager metric 'managed_services_api_rhose_operation_success_count_total{operation="provision",resource="processor",}' count is at least 1
    And the Manager metric 'managed_services_api_rhose_operation_success_count_total{operation="delete",resource="processor",}' count is at least 1
