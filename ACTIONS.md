# Actions

In SmartEvents, an `Action` is the ability to "do something" when an `Event` is matched by a [Filter](FILTERS.md) of a `Processor`.

Processors containing actions are called **"sink processors"**.

It is **not possible** to create a processor containing an `Action` and a [Source](SOURCES.md) at the same time.

When a new sink `Processor` is requested using the endpoint `/api/v1/bridges/{id}/processors` you must specify the `Action` that should be invoked, should the [Filter](FILTERS.md) of the `Processor` match.
It is possible to [Transform](TRANSFORMATIONS.md) the original `Event` structure before your `Action` is invoked, otherwise the `Event` will be passed to your `Action` unchanged.

## Parameters of an Action

Each `Action` has 2 parameters to specify:
- `type`: the type of the `Action`. This must be one of the supported `Action` types listed below
  - Attempting to use an unknown `Action` type will result in an Error from the Bridge API.
- `parameters`: A key/value map of configuration parameters for the `Action`
  - Only string for the `key` and `value` of the parameters are supported.
  - The required parameters are `Action` specific and documented in the list of supported `Actions`

## Supported Action Types

The following `Actions` are currently supported by Event Bridge:

### KafkaTopic

Allows you to send an `Event` to a Kafka Topic on a RHOSAK Kafka Cluster

#### KafkaTopic Configuration Parameters

* `topic` - The topic name to send the Event to
* `kafka_broker_url` - The RHOSAK Broker URL
* `kafka_client_id` - The Client Id part of the credentials to authenticate to Kafka
* `kafka_client_secret` - The Client Secret part of the credentials to authenticate to Kafka

#### KafkaTopic Example

To send an Event to the Kafka Topic `myRequestedTopic` on the Kafka Server `kafka-server-url:443` with the credentials `clientId` and `clientSecret`

```json
{
  "action": {
    "type": "kafka_topic_sink_0.1",
    "parameters": {
      "topic": "myRequestedTopic",
      "kafka_broker_url": "kafka-server-url:443",
      "kafka_client_id": "clientId",
      "kafka_client_secret": "clientSecret"
    }
  }
}
```

### Webhook

Allows you to send a WebHook to an endpoint of your choice. The configured endpoint will be called via an HTTP POST.

#### Webhook Configuration Parameters

- `endpoint` - The FQDN of the endpoint to invoke for the WebHook

#### Webhook Example

To send an HTTP POST to `https://example.com/my-webhook-endpoint`:

```json
{
  "action": {
    "type": "webhook_sink_0.1",
    "parameters": {
      "endpoint": "https://example.com/my-webhook-endpoint"
    }
  }
}
```

### SendToBridge

Allows you to forward an Event to any SmartEvents Instance in your account. Sending events to an SmartEvents instance not in your
account is not currently supported.

#### SendToBridge Configuration Parameters

- `bridgeId` - Set the `id` of the bridge instance to forward the Event to 

**WARNING:** There is currently no circuit breaker and your event could end up in an infinite loop.
**Please do not use the `bridgeId` omission without a transformation in your processor.**

#### SendToBridge Example

To send an event to bridge with id `foo` in my account:

```json
{
  "action": {
    "type": "send_to_bridge_sink_0.1",
    "parameters": {
      "bridgeId": "foo"
    }
  }
}
```

### Slack

Allows you to send a message to a Slack Channel of your choice

#### Slack Configuration Parameters

- `slack_channel` - The Slack Channel to send the message to
- `slack_webhook_url` - The webhook URL for the Slack Channel

#### Slack Example

To send an Event to channel `foo` with webhook URL `https://example.com`:

```json
{
  "action": {
    "type": "slack_sink_0.1",
    "parameters": {
      "slack_channel": "foo",
      "slack_webhook_url": "https://example.com"
    }
  }
}
```
