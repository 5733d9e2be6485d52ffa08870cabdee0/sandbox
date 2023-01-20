Feature: Bridge tests

  Scenario: Create a Bridge and Delete a Bridge
    Given authenticate against Manager
    When create a new Bridge "mybridge" in cloud provider "aws" and region "us-east-1"
    Then the list of Bridge instances is containing the Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    When delete the Bridge "mybridge"
    Then the Bridge "mybridge" is not existing within 4 minutes

