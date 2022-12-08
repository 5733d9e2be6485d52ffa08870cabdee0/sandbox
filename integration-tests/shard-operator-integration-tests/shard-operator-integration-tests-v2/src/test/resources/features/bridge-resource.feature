Feature: BridgeResource tests

  Background:
    Given create Namespace

  Scenario: Create bridge resource
    And create a new Kafka topic "topic1"
    And create secret "test-bridge" with data:
      | bootstrap.servers | ${env.it.shard.kafka.bootstrap.servers} |
      | user              | ${env.it.shard.kafka.user}              |
      | password          | ${env.it.shard.kafka.password}          |
      | protocol          | SASL_SSL                                |
      | topic.name        | ${topic.topic1}                         |
      | sasl.mechanism    | PLAIN                                   |

    And deploy BridgeResource "test-bridge" using topic "topic1":
    """
    apiVersion: com.redhat.service.smartevents/v2alpha1
    kind: ManagedBridge
    metadata:
      name: test-bridge
      labels:
        app.kubernetes.io/managed-by: smartevents-fleet-shard-operator
        app.kubernetes.io/created-by: smartevents-fleet-shard-operator
    spec:
      id: c8f87904-7174-11ed-a1eb-0242ac120002
      name: test-bridge
      customerId: user
      owner: user
      dnsConfiguration:
        host: my-bridge
        tls:
          key: test
          certificate: test
      kNativeBrokerConfiguration:
        kafkaConfiguration:
          bootstrapServers: ${env.kafka.bootstrap.servers}
          user: ${env.kafka.ops.client.id}
          password: ${env.kafka.ops.client.secret}
          saslMechanism: SASL_SSL
          topic: "${topic.topic1}"
          securityProtocol: PLAINTEXT
    """
    And the Ingress "test-bridge" is available within 1 minute
