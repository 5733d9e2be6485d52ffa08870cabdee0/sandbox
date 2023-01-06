#!/bin/bash

source common_tools.sh

echo "Installing External Secrets Operator"
# based on https://github.com/external-secrets/external-secrets
install_operator_and_wait external-secrets/externalSecretsSub.yaml

# set up External Secrets resources
waitForSuccess 10 oc apply -k external-secrets

echo "All dependencies installed"
