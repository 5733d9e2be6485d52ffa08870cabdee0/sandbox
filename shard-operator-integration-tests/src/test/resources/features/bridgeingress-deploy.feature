Feature: BridgeIngress deploy and undeploy

  Background:
    Given create Namespace

  # Using "dummy" image as real BridgeIngress image requires Kafka
  Scenario: BridgeIngress is in condition Ready
    When deploy BridgeIngress with default secret:
    """
    apiVersion: com.redhat.service.bridge/v1alpha1
    kind: BridgeIngress
    metadata:
      name: my-bridge-ingress
      labels:
          app.kubernetes.io/managed-by: bridge-fleet-shard-operator
    spec:
      bridgeName: my-bridge
      customerId: customer
      id: my-bridge-ingress
    """
     
     Then the BridgeIngress "my-bridge-ingress" exists within 1 minute
     And the BridgeIngress "my-bridge-ingress" is in condition "Ready" within 2 minutes

  # Using "dummy" image as real BridgeIngress image requires Kafka
  Scenario: BridgeIngress gets deleted
    Given deploy BridgeIngress with default secret:
    """
    apiVersion: com.redhat.service.bridge/v1alpha1
    kind: BridgeIngress
    metadata:
      name: my-deleted-bridge-ingress
      labels:
          app.kubernetes.io/managed-by: bridge-fleet-shard-operator
    spec:
      bridgeName: my-bridge
      customerId: customer
      id: my-bridge-ingress
    """
    And the BridgeIngress "my-deleted-bridge-ingress" is in condition "Ready" within 2 minutes

    When delete BridgeIngress "my-deleted-bridge-ingress"
     
    Then the BridgeIngress "my-deleted-bridge-ingress" does not exist within 1 minute
