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
        "type": "Webhook"
        "parameters": {
            "endpoint": "https://webhook.site/${env.webhook.site.uuid}"
       }
      }
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
        "type": "StringEquals",
        "key": "source",
        "value": "StorageService"
       }
      ]
    }
    """
    And the Processor "myProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    And send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
    "specversion": "1.0",
    "type": "StringEquals",
    "source": "StorageService",
    "id": "filter-event-test",
    "data": {
        "name": "Hello world"
      }
    }
    """
    Then Webhook site contains request with text "${cloud-event.filter-event-test.id}" within 1 minute


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
        "type": "StringEquals",
        "key": "source",
        "value": "StorageService"
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
    And send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
    "specversion": "1.0",
    "type": "StringContains",
    "source": "StorageService",
    "id": "filter-event-test-second",
    "data": {
        "name": "John"
      }
    }
    """
    Then Webhook site contains request with text "${cloud-event.filter-event-test-second.id}" within 1 minute
