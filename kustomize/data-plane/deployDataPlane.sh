#!/bin/bash

source common_tools.sh

oc get project istio-system
status=$?
if [ $status -ne 0 ]; then
  echo "The istio-system project does not exist, creating"
  oc new-project istio-system
fi

echo "Deploying all resources"
oc kustomize . | oc apply -f -

status=$?
if [ $status -ne 0 ]; then
  echo "Some resources fail to deploy (concurrency issues), redeploying"
  sleep 5
  oc kustomize . | oc apply -f -
fi

echo "Waiting for operator CSV to appear"
OPERATOR_CSV=$(waitForOpResult 60 oc get subscription.operators.coreos.com smart-events-shard-operator-subscription -n openshift-operators -o jsonpath={.status.currentCSV})

echo "Setting operator parameters in CSV template"
# deployment would get overwritten by operator CSV, need to patch CSV deployment template
waitForSuccess 20 oc patch ClusterServiceVersion "$OPERATOR_CSV" -n openshift-operators --patch-file shard/operator-csv-patch.json --type json

sleep 5

echo "Waiting for the operator installation to finish"
wait_until_operator_installed smart-events-shard-operator-subscription
