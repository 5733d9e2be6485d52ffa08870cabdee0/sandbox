#!/bin/bash

configuration_profile="$1"

# helper functions
function check_required_variable {
  var_name="$1"
  [ -z "$( eval "echo -n \$${var_name}")" ] && die "ERROR: required environment variable \"${var_name}\" is missing or empty" || true
}

function die {
  echo "$1"
  exit
}

# make sure this script is sourced (no direct execution)
[[ "${BASH_SOURCE[0]}" != "${0}" ]] || die "Script ${BASH_SOURCE[0]} is intended only to be sourced from other scripts"

# exit on first error
set -e

# list of required tools
required_tools="
jq
kubectl
kustomize
mvn
rhoas
"

# check if required tools are installed
for tool in $required_tools; do
  which "$tool" &> /dev/null || die "ERROR: required tool \"$tool\" is missing"
done

# configure profiles
function configure {
  profile=$1
  case $profile in
    kafka)
      configure_kafka
    ;;
    minikube)
      configure_minikube
    ;;
  esac
}

function configure_kafka {
  check_required_variable "MANAGED_KAFKA_INSTANCE_NAME"
}

function configure_minikube {
  MINIKUBE_PROFILE="${MINIKUBE_PROFILE:-minikube}"
  echo "MINIKUBE_PROFILE: $MINIKUBE_PROFILE"
}

for configuration_profile in "$@"; do configure "$configuration_profile"; done
