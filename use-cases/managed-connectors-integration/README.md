## Managed Connectors Integration

An example of how to interact with Managed Connectors. It will create the needed service account on Managed Kafka, the needed Kafka instance with the correct permissions and the connector that will write to the Slack channel `#mc` the messages published on the `slacktopic` Kafka topic.

### Prerequisites:

* [kcat](https://github.com/edenhill/kcat) 
* [rhoas](https://access.redhat.com/documentation/en-us/red_hat_openshift_streams_for_apache_kafka/1/guide/f520e427-cad2-40ce-823d-96234ccbc047)
* A [Slack app](https://api.slack.com/messaging/webhooks) with a webhook created exported as a `$WEBHOOK_URL` variable

### Usage:

```shell
# Only if needed
# If you don't create a service account like this provide a ./service-acct-credentials.json as in `create-service-account.sh`
./create-service-account.sh

# Then
./create-kafka-instance.sh
# Follow the output of the script, it will set the needed variables

source scripts/token.sh

export WEBHOOK_URL=<slack URL>

mvn compile exec:java -Dexec.mainClass=com.redhat.service.ManagedConnectorServiceApplication -Dexec.args="$OCM_TOKEN $COS_BASE_PATH $KAFKA_BASE_PATH $SERVICEACCOUNT_ID $SERVICEACCOUNT_SECRET $WEBHOOK_URL"

kcat -t slacktopic -b "$KAFKA_BASE_PATH" \
-X security.protocol=SASL_SSL -X sasl.mechanisms=PLAIN \
-X sasl.username="$SERVICEACCOUNT_ID" \
-X sasl.password="$SERVICEACCOUNT_SECRET" -P

```
