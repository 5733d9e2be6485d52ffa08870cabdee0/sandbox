# Source Processors Demo

This document explains how _source processors_ work and how to configure a simple local demo using
a _SlackSource_ and a _SlackAction_ (called _"From Slack To Slack"_).

## How to define a source processor

The payload to define a source processor is similar to a sink processor, with two differences:

* It contains a _"source"_ field instead of _"action"_. The internal structure is the same, with _"type"_ and _"parameters"_.
* It currently ignores any specified _"transformationTemplate"_.

Filters work exactly in the same way.

More information on available sources can be found in the [dedicated document](../SOURCES.md).

### Example payload to create a source processor

```json
{
  "name": "slack-source",
  "source": {
    "type": "Slack",
    "parameters": {
      "channel": "my-read-channel",
      "token": "xoxb-..."
    }
  },
  "filters": [
    {
      "key": "data.text",
      "type": "StringContains",
      "values": [ "hello", "hi" ]
    }
  ]
}
```

## "From Slack To Slack" demo

This demo reads messages from a Slack channel and, for each one, posts a message to another Slack channel containing
the user ID and the text.

It uses one source processor containing a _SlackSource_ and one sink processor containing a _SlackAction_
(thus the name _"From Slack To Slack"_).

### Preliminary assumption

* Access to working RHOSE installation (either local via minikube or on OSD), correctly configured to create Managed Connectors.
* An existing bridge instance in `ready` state.
* A Slack instance and Slack app configured to access it, with both a [bot token](https://api.slack.com/authentication/token-types#bot) and a [webhook](https://api.slack.com/messaging/webhooks). _Ask the dev team for help if you need._

### Create the source processor

This is the payload to create the source processor. Just set then `channel` parameter with the name of the channel
you want to read from and set the correct `token`. Here is the JSON:

```json
{
  "name": "slack-source",
  "source": {
    "type": "Slack",
    "parameters": {
      "channel": "my-read-channel",
      "token": "xoxb-..."
    }
  }
}
```

### Create the sink processor

This is the payload to create the sink processor. Just set then `channel` parameter with the name of the channel
you want to write to and set the correct `webhookUrl`. Here is the JSON:

```json
{
  "name": "slack-sink",
  "action": {
    "type": "Slack",
    "parameters": {
      "channel": "my-write-channel",
      "webhookUrl": "https://hooks.slack.com/services/..."
    }
  },
  "filters": [
    {
      "key": "type",
      "type": "StringEquals",
      "value": "SlackSource"
    }
  ],
  "transformationTemplate": "Slack user {data.user} says \"{data.text}\""
}
```

### Test it!

After the two processors are in `ready` state, try writing a new message in `my-read-channel` and you should see a
new message in `my-write-channel`.