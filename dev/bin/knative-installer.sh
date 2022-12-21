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

header_text "Installing Istio Configuration for Knative Serving"
# This command _should_ only install the CRDS first using the label selector. However it seems to create other resources
# so we add the "|| true" statement on the end to force a 0 exit code and not break the script. We then run the apply
# command again and verify that all resources have come up correctly so this approach is ~kinda safe.
kubectl apply -l knative.dev/crd-install=true -f https://github.com/knative/net-istio/releases/download/knative-v1.5.0/istio.yaml || true
kubectl apply -f https://github.com/knative/net-istio/releases/download/knative-v1.5.0/istio.yaml

# We running in local dev, we don't need multiple replicas or HPA for the Istio Components
kubectl scale --replicas=1 deployment istio-ingressgateway -n istio-system
kubectl delete hpa istiod -n istio-system --ignore-not-found=true
kubectl scale --replicas=1 deployment istiod -n istio-system

header_text "Waiting for all Istio resources to become ready..."
kubectl wait deployment --all --timeout=900s --for=condition=Available -n istio-system
kubectl apply -f https://github.com/knative/net-istio/releases/download/knative-v1.5.0/net-istio.yaml
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-serving
kubectl label namespace knative-serving istio-injection=enabled --overwrite

# Sets the domain name configuration for all KService services to be exposed as cluster private. We do not need to
# expose Kservice to the outside world.
# @see https://knative.dev/docs/serving/services/private-services/
kubectl patch configmap/config-domain \
      --namespace knative-serving \
      --type merge \
      --patch '{"data":{"svc.cluster.local":""}}'

header_text "Knative Eventing Kafka - Installer"
header_text "Initializing Knative Eventing Core APIs"
kubectl apply -f https://github.com/knative/eventing/releases/download/knative-v1.5.6/eventing-crds.yaml
kubectl apply -f https://github.com/knative/eventing/releases/download/knative-v1.5.6/eventing-core.yaml

header_text "Waiting for Knative Eventing Core to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-eventing

header_text "Initializing Knative Eventing Kafka APIs"
kubectl apply -f https://github.com/knative-sandbox/eventing-kafka-broker/releases/download/knative-v1.5.8/eventing-kafka-controller.yaml
kubectl apply -f https://github.com/knative-sandbox/eventing-kafka-broker/releases/download/knative-v1.5.8/eventing-kafka-broker.yaml
kubectl apply -f https://github.com/knative-sandbox/eventing-kafka-broker/releases/download/knative-v1.5.8/eventing-kafka-sink.yaml
kubectl apply -f https://github.com/knative-sandbox/eventing-kafka-broker/releases/download/knative-v1.5.8/eventing-kafka-source.yaml

header_text "Waiting for Knative Eventing Kafka to become ready"
kubectl wait deployment --all --timeout=900s --for=condition=Available -n knative-eventing