# Actions

In EventBridge, an `Action` is the ability to "do something" when an `Event` is matched by a [Filter](FILTERS.md) of a `Processor`

When a new `Processor` is requested using the endpoint `/api/v1/bridges/{id}/processors` you must specify the `Action` that should be invoked, should the [Filter](FILTERS.md) of the `Processor` match. 
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

### SendToKafka

Allows you to send an `Event` to a Kafka Topic on a hard-coded Kafka Cluster made available via the Event Bridge deployment

#### Configuration Parameters

* `topic` - The topic name to send the Event to

#### Example

To send an Event to the topic `myRequestedTopic`:

```json
{
  "action": {
    "type": "SendToKafka",
    "parameters": {
      "topic": "myRequestedTopic"
    }
  }
}
```
### Webhook

Allows you to send a WebHook to an endpoint of your choice. The configured endpoint will be called via an HTTP POST.

#### Configuration Parameters

* `endpoint` - The FQDN of the endpoint to invoke for the WebHook

#### Example

To send an HTTP POST to `https://example.com/my-webhook-endpoint`:

```json
{
  "action": {
    "type": "Webhook",
    "parameters": {
      "endpoint": "https://example.com/my-webhook-endpoint"
    }
  }
}
```

### SendToBridge

Allows you to forward an Event to any EventBridge Instance in your account. Sending events to an EventBridge instance not in your
account is not currently supported.

#### Configuration Parameters

* `bridgeId` - An optional property for the `id` of the bridge instance to forward the Event to
  * If the `bridgeId` configuration property is omitted, then the bridge instance on which the Processor exists is the target 

#### Example

To send an event to bridge with id `foo` in my account:

```json
{
  "action": {
    "type": "SendToBridge",
    "parameters": {
      "bridgeId": "foo"
    }
  }
}
```

### SlackAction

Allows you to send a message to a Slack Channel of your choice

#### Configuration Parameters

* `channel` - The Slack Channel to send the message to
* `webhookUrl` - The webhook URL for the Slack Channel

#### Example

To send an Event to channel `foo` with webhook URL `https://example.com`:

```json
{
  "action": {
    "type": "SlackAction",
    "parameters": {
      "channel": "foo",
      "webhookUrl": "https://example.com"
    }
  }
}
```
