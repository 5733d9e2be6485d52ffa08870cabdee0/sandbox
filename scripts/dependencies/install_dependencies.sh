#!/bin/bash

source common_tools.sh

echo "Installing Serverless Operator"
# based on https://docs.openshift.com/container-platform/4.11/serverless/install/install-serverless-operator.html
install_operator_and_wait serverless/serverlessSub.yaml
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
