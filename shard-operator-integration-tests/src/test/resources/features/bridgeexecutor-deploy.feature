Feature: BridgeExecutor deploy and undeploy

  Background:
    Given create Namespace

  # Using "dummy" image as real BridgeExecutor image requires Kafka
  Scenario: BridgeExecutor is in condition Ready
    When deploy BridgeExecutor with default secret:
    """
    apiVersion: com.redhat.service.bridge/v1alpha1
    kind: BridgeExecutor
    metadata:
      name: my-bridge-executor
      labels:
          app.kubernetes.io/managed-by: bridge-fleet-shard-operator
    spec:
      image: quay.io/5733d9e2be6485d52ffa08870cabdee0/empty-it-image:1.0
      id: my-bridge-executor-id
      bridgeId: my-bridge-id
      customerId: customer
      owner: customer
      processorType: sink
      processorDefinition: '{"filters":[{"type":"StringEquals","type":"StringEquals","key":"data.name","value":"test"}],"transformationTemplate":null,"requestedAction":{"type":"webhook_sink_0.1","parameters":{"endpoint":"https://webhook.site/xxxxxx"}},"requestedSource":null,"resolvedAction":{"type":"webhook_sink_0.1","parameters":{"endpoint":"https://webhook.site/xxxxxx"}}}'
      processorName: mybridge-processor
    """

    Then the BridgeExecutor "my-bridge-executor" exists within 2 minute
    And the Deployment "my-bridge-executor" is ready within 2 minute
    And the Service "my-bridge-executor" exists within 2 minute
    And the BridgeExecutor "my-bridge-executor" is in condition "Ready" within 3 minutes

  # Using "dummy" image as real BridgeExecutor image requires Kafka
  Scenario: BridgeExecutor gets deleted
    Given deploy BridgeExecutor with default secret:
    """
    apiVersion: com.redhat.service.bridge/v1alpha1
    kind: BridgeExecutor
    metadata:
      name: my-deleted-bridge-executor
      labels:
          app.kubernetes.io/managed-by: bridge-fleet-shard-operator
    spec:
      image: quay.io/5733d9e2be6485d52ffa08870cabdee0/empty-it-image:1.0
      id: my-bridge-executor-id
      bridgeId: my-bridge-id
      customerId: customer
      owner: customer
      processorType: sink
      processorDefinition: '{"filters":[{"type":"StringEquals","type":"StringEquals","key":"data.name","value":"test"}],"transformationTemplate":null,"requestedAction":{"type":"webhook_sink_0.1","parameters":{"endpoint":"https://webhook.site/xxxxxx"}},"requestedSource":null,"resolvedAction":{"type":"webhook_sink_0.1","parameters":{"endpoint":"https://webhook.site/xxxxxx"}}}'
      processorName: mybridge-processor
    """

    And the BridgeExecutor "my-deleted-bridge-executor" is in condition "Ready" within 2 minutes

    When delete BridgeExecutor "my-deleted-bridge-executor"

    Then the BridgeExecutor "my-deleted-bridge-executor" does not exists within 1 minute
    And no Deployment exists within 1 minute
    And no Service exists within 1 minutes
