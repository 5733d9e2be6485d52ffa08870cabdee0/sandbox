Feature: Kafka Action tests

  Background:
    Given authenticate against Manager

  @kafkaaction
  Scenario: Send message to Kafka topic using the Kafka action
    And create a new Kafka topic "topic1"
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes
    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "type": "kafka_topic_sink_0.1",
        "parameters": {
            "topic": "${topic.topic1}",
            "kafka_broker_url": "${env.kafka.bootstrap.servers}",
            "kafka_client_id": "${env.kafka.ops.client.id}",
            "kafka_client_secret": "${env.kafka.ops.client.secret}"
        }
      },
      "transformationTemplate" : "{\"data\": \"{data}\"}"
    }
    """
    And the list of Processor instances of the Bridge "mybridge" is containing the Processor "myProcessor"
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes

    When send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
      "specversion": "1.0",
      "type": "kafka.message.send",
      "source": "KafkaActionTestService",
      "id": "kafka-action-test",
      "data": {
          "message1": "${uuid.message1}"
        }
    }
    """
    Then Kafka topic "topic1" contains message with text of "message1=${uuid.message1}"
