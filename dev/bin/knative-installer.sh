#!/usr/bin/env bash

set -e

# Turn colors in this script off by setting the NO_COLOR variable in your
# environment to any value:
#
# $ NO_COLOR=1 knative-setup.sh
NO_COLOR=${NO_COLOR:-""}
if [ -z "$NO_COLOR" ]; then
  header=$'\e[1;33m'
  reset=$'\e[0m'
else
  header=''
  reset=''
fi

CONFIGURE_SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`
# The Knative Core APIs, like Broker or Trigger
eventing_core_url=${CONFIGURE_SCRIPT_DIR_PATH}/knative/resources/knative-eventing.yaml
# Knative Kafka offering: All Kafka centric APIs
eventing_kafka_url=${CONFIGURE_SCRIPT_DIR_PATH}/knative/resources/knative-kafka.yaml

function header_text {
  echo "$header$*$reset"
}

header_text "Knative Eventing Kafka - Installer"

header_text "Initializing Knative Eventing Core APIs"
kubectl apply --filename $eventing_core_url

header_text "Waiting for Knative Eventing Core to become ready"
kubectl wait deployment --all --timeout=-1s --for=condition=Available -n knative-eventing

header_text "Initializing Knative Eventing Kafka APIs"
kubectl apply --filename $eventing_kafka_url

header_text "Patch the deployment to disable Source/Channel..."
kubectl patch deployment \
  kafka-controller \
  --namespace knative-eventing \
  --type='json' \
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args", "value": [
  "--disable-controllers=source-controller,channel-controller"
]}]'

header_text "Waiting for Knative Eventing Kafka to become ready"
kubectl wait deployment --all --timeout=-1s --for=condition=Available -n knative-eventing

header_text "Registering 'Kafka' as default Knative Eventing Broker"
cat <<-EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: config-br-defaults
  namespace: knative-eventing
data:
  default-br-config: |
    clusterDefault:
      brokerClass: Kafka
      apiVersion: v1
      kind: ConfigMap
      name: kafka-broker-config
      namespace: knative-eventing
EOF