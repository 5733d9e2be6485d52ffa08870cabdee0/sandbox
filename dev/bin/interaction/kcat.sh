#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

echo "Connecting to $KAFKA_BROKER_URL with id: $KAFKA_CLIENT_ID"

kcat -t "$KAFKA_TOPIC" -b "$KAFKA_BROKER_URL" \
-X security.protocol=SASL_SSL -X sasl.mechanisms=PLAIN \
-X sasl.username="$KAFKA_CLIENT_ID" \
-X sasl.password="$KAFKA_CLIENT_SECRET" -C