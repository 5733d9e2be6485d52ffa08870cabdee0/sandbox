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

function header_text {
  echo "$header$*$reset"
}

header_text "Knative Eventing Kafka - Installer"
header_text "Initializing Knative Eventing Core APIs"
kubectl apply -f kubectl apply -f https://github.com/knative/eventing/releases/download/knative-v1.5.6/eventing-crds.yaml
kubectl apply -f https://github.com/knative/eventing/releases/download/knative-v1.5.6/eventing-core.yaml

header_text "Waiting for Knative Eventing Core to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-eventing

header_text "Initializing Knative Eventing Kafka APIs"
kubectl apply -f https://github.com/knative-sandbox/eventing-kafka-broker/releases/download/knative-v1.5.8/eventing-kafka-controller.yaml
kubectl apply -f https://github.com/knative-sandbox/eventing-kafka-broker/releases/download/knative-v1.5.8/eventing-kafka-broker.yaml

header_text "Waiting for Knative Eventing Kafka to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-eventing
