#!/bin/bash

source common_tools.sh

echo "Removing ServiceMesh resources"
waitForSuccess 3 oc kustomize servicemesh | oc delete --ignore-not-found=true -f -

echo "Removing ServiceMesh Operators"
# according to https://docs.openshift.com/container-platform/4.11/service_mesh/v2x/installing-ossm.html
# the individual operators have to be installed in this order and each one only after the previous one's installation finished
uninstall_operator servicemesh/serviceMeshSub.yaml
uninstall_operator servicemesh/kialiSub.yaml
uninstall_operator servicemesh/tracingSub.yaml

echo "Removing Serverless resources"
waitForSuccess 3 oc kustomize serverless | oc delete --ignore-not-found=true -f -

echo "Removing Serverless Operator"
# based on https://docs.openshift.com/container-platform/4.11/serverless/install/install-serverless-operator.html
uninstall_operator serverless/serverlessSub.yaml
