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

header_text "Initializing Knative Serving Core APIs"
kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.5.0/serving-crds.yaml
kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.5.0/serving-core.yaml

header_text "Waiting for Knative Serving Core to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-serving

header_text "Initializing Kourier"
kubectl apply -f https://github.com/knative/net-kourier/releases/download/knative-v1.5.0/kourier.yaml

header_text "Waiting for Kourier to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n kourier-system

header_text "Patching Knative Serving config to use Kourier"
kubectl patch configmap/config-network \
  --namespace knative-serving \
  --type merge \
  --patch '{"data":{"ingress-class":"kourier.ingress.networking.knative.dev"}}'

header_text "Knative Eventing Kafka - Installer"
header_text "Initializing Knative Eventing Core APIs"
kubectl apply -f https://github.com/knative/eventing/releases/download/knative-v1.5.6/eventing-crds.yaml
kubectl apply -f https://github.com/knative/eventing/releases/download/knative-v1.5.6/eventing-core.yaml

header_text "Waiting for Knative Eventing Core to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-eventing

header_text "Initializing Knative Eventing Kafka APIs"
kubectl apply -f https://github.com/knative-sandbox/eventing-kafka-broker/releases/download/knative-v1.5.8/eventing-kafka-controller.yaml
kubectl apply -f https://github.com/knative-sandbox/eventing-kafka-broker/releases/download/knative-v1.5.8/eventing-kafka-broker.yaml

header_text "Waiting for Knative Eventing Kafka to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-eventing

header_text "Initializing Knative Eventing in-memory channel"
kubectl apply -f https://github.com/knative/eventing/releases/download/knative-v1.5.6/in-memory-channel.yaml

header_text "Waiting for Knative Eventing in-memory channel to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-eventing

header_text "Initializing Knative Eventing multi-tenant channel-based broker"
kubectl apply -f https://github.com/knative/eventing/releases/download/knative-v1.5.6/mt-channel-broker.yaml

header_text "Waiting for Knative Eventing multi-tenant channel-based broker to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-eventing
