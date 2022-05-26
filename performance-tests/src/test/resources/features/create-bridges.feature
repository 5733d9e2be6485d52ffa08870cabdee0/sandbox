Feature: Bridge tests

  Scenario: Bridge is created and correctly deleted
    Given authenticate against Manager
    And start and wait for benchmark "benchmarks/rhose-create-bridges.hf.yaml" in Hyperfoil
