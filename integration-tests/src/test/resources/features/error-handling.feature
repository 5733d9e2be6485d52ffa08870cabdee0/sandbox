Feature: Error handling tests

  @errorhandlingdlqheaders
  Scenario: A failure occurs and a cloud event message is sent by managed connector to DLQ with correct headers
    Given authenticate against Manager
    And create a new Bridge with body:
    """
    {
        "name": "ehBridge",
        "cloud_provider": "aws",
        "region": "us-east-1",
        "error_handler": {
            "type": "webhook_sink_0.1",
            "parameters": {
                "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
            }
        }
    }
    """
    And the Bridge "ehBridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "ehBridge" is available within 2 minutes

    And create an SQS queue on AWS called "my-sqs-queue"

    And add a Processor to the Bridge "ehBridge" with body:
    """
    {
       "name": "sqsProcessor",
       "source": {
          "type": "aws_sqs_source_0.1",
          "parameters": {
                "aws_queue_name_or_arn": "${aws.sqs.my-sqs-queue}",
                "aws_region": "${aws.region}",
                "aws_access_key" : "${aws.access-key}",
                "aws_secret_key" : "${aws.secret-key}",
                "aws_delay": 5000
          }
       }
    }
    """
    And the Processor "sqsProcessor" of the Bridge "ehBridge" is existing with status "ready" within 3 minutes
    And the Processor "sqsProcessor" of the Bridge "ehBridge" has source of type "aws_sqs_source_0.1"

    When send a message with text "invalid message for bridge ${bridge.ehBridge.id}" to "my-sqs-queue" sqs queue

    Then Webhook site with id "${env.webhook.site.uuid}" contains request with text "invalid message for bridge ${bridge.ehBridge.id}" within 1 minute and headers:
      | x-rhose-processor-id          | ${bridge.ehBridge.processor.sqsProcessor.id} |
      | x-rhose-original-event-source | RHOSE                                        |
      | x-rhose-original-event-id     |                                              |
      | x-rhose-error-id              | OPENBRIDGE-6                                 |
      | x-rhose-bridge-id             | ${bridge.ehBridge.id}                        |
      | x-dead-letter-topic           |                                              |
      | x-dead-letter-reason          |                                              |
      | x-dead-letter-partition       |                                              |
      | x-dead-letter-offset          |                                              |