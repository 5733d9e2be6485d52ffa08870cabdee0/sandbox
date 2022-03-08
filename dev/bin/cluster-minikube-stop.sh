#!/bin/bash -e

########
# Stop the minikube cluster
#
# Env vars:
# - MINIKUBE_PROFILE: set the current minikube profile (optional, default="minikube")
########

. $(dirname "${BASH_SOURCE[0]}")/common.sh

configure_minikube

set -x

minikube -p "${minikube_profile}" stop

set +x