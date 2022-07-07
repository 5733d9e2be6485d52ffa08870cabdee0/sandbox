#!/bin/bash

echo "Removing Data Plane resources"
# Delete the operator CSV first
NAMESPACE=openshift-operators
SUBSCRIPTION=smart-events-shard-operator-subscription
CSV=`oc get subscription.operators.coreos.com $SUBSCRIPTION -n $NAMESPACE -o jsonpath={.status.installedCSV}`
oc delete subscription.operators.coreos.com $SUBSCRIPTION -n $NAMESPACE
oc delete csv $CSV -n $NAMESPACE

# Delete remaining resources
oc kustomize . | oc delete --ignore-not-found=true -f -
