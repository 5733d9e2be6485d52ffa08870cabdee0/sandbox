#!/bin/bash

source common_tools.sh

echo "Removing Data Plane operator"
uninstall_operator shard/operator-subscription.yaml

# Delete remaining resources
waitForSuccess 3 oc kustomize . | oc delete --ignore-not-found=true -f -
