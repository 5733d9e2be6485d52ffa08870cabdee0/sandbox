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

# The Knative Core APIs, like Broker or Trigger
eventing_core_url=https://raw.githubusercontent.com/openshift/knative-eventing/release-v1.1/openshift/release/knative-eventing-ci.yaml
# Knative Kafka offering: All Kafka centric APIs
eventing_kafka_url=https://gist.githubusercontent.com/matzew/82280d23baa6cfd6c332c2325f5785c9/raw/848e2d9b36b39c491a60ba24f4ce167201a218ef/knative-kafka.yaml

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
