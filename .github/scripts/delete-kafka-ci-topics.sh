#!/bin/bash

CLEAN_UP_PREFIX="ci-"

if [[ -z "$BOOSTRAP_SERVER" ]]
then
    echo "Mandatory environment variable BOOSTRAP_SERVER is missing"
    exit 1
fi
if [[ -z "$KAFKA_ADMIN_USERNAME" ]]
then
    echo "Mandatory environment variable KAFKA_ADMIN_USERNAME is missing"
    exit 1
fi
if [[ -z "$KAFKA_ADMIN_PASSWORD" ]]
then
    echo "Mandatory environment variable KAFKA_ADMIN_PASSWORD is missing"
    exit 1
fi

mkdir kafka-clients

pushd kafka-clients
curl https://archive.apache.org/dist/kafka/3.1.0/kafka_2.13-3.1.0.tgz > kafka.tgz
tar -xvf kafka.tgz --strip=1

echo "security.protocol=SASL_SSL" > kafka-connection-properties.txt
echo "sasl.mechanism=PLAIN" >> kafka-connection-properties.txt
echo "sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$KAFKA_ADMIN_USERNAME\" password=\"$KAFKA_ADMIN_PASSWORD\";" >> kafka-connection-properties.txt

topics=$(./bin/kafka-topics.sh --bootstrap-server "$BOOSTRAP_SERVER" --list --command-config kafka-connection-properties.txt | grep "$CLEAN_UP_PREFIX")
for topic in $topics
do
    echo "Deleting topic: $topic"
    ./bin/kafka-acls.sh --bootstrap-server "$BOOSTRAP_SERVER" --remove --topic "$topic" --force --command-config kafka-connection-properties.txt
    ./bin/kafka-topics.sh --bootstrap-server "$BOOSTRAP_SERVER" --delete --topic "$topic" --command-config kafka-connection-properties.txt
done
popd
