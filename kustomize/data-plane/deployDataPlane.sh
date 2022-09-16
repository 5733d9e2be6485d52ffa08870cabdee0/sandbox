#!/bin/bash

source common_tools.sh

echo "Deploying catalog resources and addon parameters secret"
oc kustomize . | oc apply -f -

echo "Installing Data Plane Fleet Shard Operator"
install_operator_and_wait shard/operator-subscription.yaml
