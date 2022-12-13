Feature: Bridge tests

  Scenario: Bridge fails to be listed as the operation is not implemented yet
    Given authenticate against Manager
    And the list of Bridge instances is failing with HTTP response code 200

  Scenario: Create a Bridge
    Given authenticate against Manager
    When create a new Bridge "mybridge" in cloud provider "aws" and region "us-east-1"
    Then the list of Bridge instances is containing the Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "provisioning" within 3 minutes

  