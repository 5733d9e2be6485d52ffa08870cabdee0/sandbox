#!/bin/bash

source common_tools.sh

echo "Installing Serverless Operator"
# based on https://github.com/openshift-knative/serverless-operator/blob/main/docs/install-midstream.md
waitForSuccess 10 oc apply -f serverless/serverless-system-namespace.yaml
waitForSuccess 10 oc apply -f serverless/serverless-operator-group.yaml
waitForSuccess 10 oc apply -f serverless/serverless-catalog-source.yaml
# TODO: do we want to wait until catalog source is ready?
# oc wait catalogsources -n openshift-marketplace serverless-operator-v1-24-0 --for=jsonpath='{.status.connectionState.lastObservedState}'="READY" --timeout=5m
install_operator_and_wait serverless/serverlessSub.yaml openshift-serverless
# set up serverless resources
waitForSuccess 10 oc apply -k serverless

echo "Installing ServiceMesh Operators"
# according to https://docs.openshift.com/container-platform/4.11/service_mesh/v2x/installing-ossm.html
# the individual operators have to be installed in this order and each one only after the previous one's installation finished
install_operator_and_wait servicemesh/tracingSub.yaml
install_operator_and_wait servicemesh/kialiSub.yaml
install_operator_and_wait servicemesh/serviceMeshSub.yaml

# set up service mesh resources
waitForSuccess 10 oc apply -k servicemesh
