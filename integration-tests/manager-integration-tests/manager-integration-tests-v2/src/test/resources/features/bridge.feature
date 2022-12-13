Feature: Bridge tests

  Scenario: Bridge fails to be listed as the operation is not implemented yet
    Given authenticate against Manager
    And the list of Bridge instances is failing with HTTP response code 200
