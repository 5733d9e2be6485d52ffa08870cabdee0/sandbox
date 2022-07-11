Feature: Bridge tests

  Scenario: Bridge is created and correctly deleted
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the list of Bridge instances is containing the Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 5 minutes
    And the Ingress of Bridge "mybridge" is available within 3 minutes

    When delete the Bridge "mybridge"

    Then the Bridge "mybridge" is not existing within 2 minutes
    And the Ingress of Bridge "mybridge" is not available within 1 minute

    And the Manager metric 'managed_services_api_rhose_operation_success_count_total{operation="provision",resource="bridge",}' count is at least 1
    And the Manager metric 'managed_services_api_rhose_operation_success_count_total{operation="delete",resource="bridge",}' count is at least 1
    And the Manager metric 'managed_services_api_rhose_operation_count_total{operation="provision",resource="bridge",}' count is at least 1
    And the Manager metric 'managed_services_api_rhose_operation_count_total{operation="delete",resource="bridge",}' count is at least 1
