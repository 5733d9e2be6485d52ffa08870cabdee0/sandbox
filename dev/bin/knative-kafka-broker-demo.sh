#!/bin/bash

########
# Full setup of test Managed Kafka instance and service accounts.
# It is idempotent and can be run multiple times.
#
# Env vars:
# - MANAGED_KAFKA_INSTANCE_NAME: set the managed kafka instance name (required)
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

. "${SCRIPT_DIR_PATH}/configure.sh" kafka

function header_text {
  echo "$header$*$reset"
}

function create_kafka_secret(){
  header_text "Create a secret for $MANAGED_KAFKA_INSTANCE_NAME Knative Kafka Broker auth against RHOSAK"

  mc_client_id=$( getManagedKafkaMcSAClientId ) || die "can't find mc json credentials. Run kafka-setup.sh to configure it."
  mc_client_secret=$( getManagedKafkaMcSAClientSecret ) || die "can't find mc json credentials. Run kafka-setup.sh to configure it."

  kubectl create secret --namespace default generic $MANAGED_KAFKA_INSTANCE_NAME \
    --from-literal=protocol=SASL_SSL \
    --from-literal=sasl.mechanism=PLAIN \
    --from-literal=user="${mc_client_id}" \
    --from-literal=password="${mc_client_secret}"
}

function create_config_map(){
  header_text "Create a ConfigMap for $MANAGED_KAFKA_INSTANCE_NAME Knative Kafka Broker"

bootstrap_server_host=$( getManagedKafkaBootstrapServerHost ) || die "can't find instance json credentials. Run kafka-setup.sh to configure it."

  cat <<EOF | oc apply -f - || return $?
apiVersion: v1
kind: ConfigMap
metadata:
  name: $MANAGED_KAFKA_INSTANCE_NAME-config
data:
  default.topic.partitions: "10"
  default.topic.replication.factor: "3"
  bootstrap.servers: ${bootstrap_server_host}
  auth.secret.ref.name: ${MANAGED_KAFKA_INSTANCE_NAME}
EOF
}

function create_broker(){
  header_text "Create the $MANAGED_KAFKA_INSTANCE_NAME Knative Kafka Broker"

  cat <<EOF | oc apply -f - || return $?
apiVersion: eventing.knative.dev/v1
kind: Broker
metadata:
  annotations:
    eventing.knative.dev/broker.class: Kafka
  name: $MANAGED_KAFKA_INSTANCE_NAME
spec:
  config:
    apiVersion: v1
    kind: ConfigMap
    name: $MANAGED_KAFKA_INSTANCE_NAME-config
EOF
}

function setup_kafka(){
  create_kafka_secret
  create_config_map
  create_broker
}

header_text "Starting to prepare for Kafka Broker for $MANAGED_KAFKA_INSTANCE_NAME"
setup_kafka
