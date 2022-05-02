Feature: Tests of Processor Filter update

  Background:
    Given authenticate against Manager
    And create a new Bridge "mybridge"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes

  Scenario: Processor filter is updated
    Given add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "type": "Webhook",
        "parameters": {
            "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
       }
      },
      "filters": [
       {
        "type": "StringEquals",
        "key": "source",
        "value": "StorageService"
       }
      ]
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes

    When update the Processor "myProcessor" of the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "type": "Webhook",
        "parameters": {
            "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
        }
      },
      "filters": [
       {
        "type": "StringBeginsWith",
        "key": "data.name",
        "values": [
                "John",
                "Marco"
            ]
       },
       {
        "type": "StringContains",
        "key": "data.name",
        "values": [
              "John",
              "Marco"
            ]
        }
      ]
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    # Need to wait until original Processor pod is completely terminated, see https://issues.redhat.com/browse/MGDOBR-613
    And wait for 10 seconds
    And send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
    "specversion": "1.0",
    "type": "StringContains",
    "source": "StorageService",
    "id": "filter-event-test",
    "data": {
        "name": "John"
      }
    }
    """
    Then Webhook site contains request with text "${cloud-event.filter-event-test.id}" within 1 minute

    And send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
    "specversion": "1.0",
    "type": "StringEquals",
    "source": "StorageService",
    "id": "filter-InvalidEvent-test",
    "data": {
        "name": "Hello world event not matching"
      }
    }
    """
    Then Webhook site does not contains request with text "${cloud-event.filter-InvalidEvent-test.id}" within 10 seconds

  Scenario: Messages are sent to Processor with matching filter

    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "myProcessor",
      "action": {
        "type": "Webhook",
        "parameters": {
            "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
        }
      },
      "filters": [
      {
        "key": "source",
        "type": "StringEquals",
        "value": "StorageService"
      }
      ]

    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes


    When send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
      "specversion": "1.0",
      "type": "webhook.site.invoked",
      "source": "StorageService",
      "id": "filter-test",
      "data": {
          "name": "world"
        }
    }
    """
    Then Webhook site contains request with text "${cloud-event.filter-test.id}" within 1 minute


    When send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
      "specversion": "1.0",
      "type": "webhook.site.invoked",
      "source": "invalid",
      "id": "filter-invalidtest",
      "data": {
          "name": "world"
        }
    }
    """

    Then Webhook site does not contains request with text "${cloud-event.filter-invalidtest.id}" within 10 seconds



