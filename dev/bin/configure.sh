#!/bin/bash

CONFIGURE_SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

configuration_profile="$1"

# helper functions
function check_required_variable {
  var_name="$1"
  [ -z "$( eval "echo -n \$${var_name}")" ] && die "required environment variable \"${var_name}\" is missing or empty" || true
}

function die {
  echo "ERROR: $1"
  exit
}

# make sure this script is sourced (no direct execution)
[[ "${BASH_SOURCE[0]}" != "${0}" ]] || die "script ${BASH_SOURCE[0]} is intended only to be sourced from other scripts"

# exit on first error
set -e

# list of required tools
required_tools="
docker-compose
jq
kubectl
kustomize
mvn
ping
rhoas
"

# check if required tools are installed
for tool in $required_tools; do
  which "$tool" &> /dev/null || die "required tool \"$tool\" is missing"
done

# load local config if found
if [ -f "${CONFIGURE_SCRIPT_DIR_PATH}/localconfig" ]; then
  . "${CONFIGURE_SCRIPT_DIR_PATH}/localconfig"
  echo "Loaded local config file"
fi

# export credentials folder path
export CREDENTIALS_FOLDER=`realpath ${CONFIGURE_SCRIPT_DIR_PATH}/credentials`
mkdir -p "${CREDENTIALS_FOLDER}"

# configure profiles
function configure {
  profile=$1
  case $profile in
    kafka)
      configure_kafka
    ;;
    managed-connectors)
      configure_managed_connectors
    ;;
    minikube)
      configure_minikube
    ;;
    minikube-started)
      configure_minikube_started
    ;;
  esac
}

function configure_kafka {
  check_required_variable "MANAGED_KAFKA_INSTANCE_NAME"
  MANAGED_KAFKA_CREDENTIALS_FILE="${CREDENTIALS_FOLDER}/${MANAGED_KAFKA_INSTANCE_NAME}.json"
  ADMIN_SA_NAME="${MANAGED_KAFKA_INSTANCE_NAME}-admin"
  ADMIN_SA_CREDENTIALS_FILE="${CREDENTIALS_FOLDER}/${ADMIN_SA_NAME}.json"
  OPS_SA_NAME="${MANAGED_KAFKA_INSTANCE_NAME}-ops"
  OPS_SA_CREDENTIALS_FILE="${CREDENTIALS_FOLDER}/${OPS_SA_NAME}.json"
  MC_SA_NAME="${MANAGED_KAFKA_INSTANCE_NAME}-mc"
  MC_SA_CREDENTIALS_FILE="${CREDENTIALS_FOLDER}/${MC_SA_NAME}.json"
}

function configure_managed_connectors {
  check_required_variable "OPENSHIFT_OFFLINE_TOKEN"
  MANAGED_CONNECTORS_CLUSTER_ID="${MANAGED_CONNECTORS_CLUSTER_ID:-empty}"
}

function configure_minikube {
  MINIKUBE_PROFILE="${MINIKUBE_PROFILE:-minikube}"
  MINIKUBE_CPUS="${MINIKUBE_CPUS:-4}"
  MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-8192}"
  MINIKUBE_KUBERNETES_VERSION="${MINIKUBE_KUBERNETES_VERSION:-v1.20.0}"
}

function configure_minikube_started {
  configure_minikube
  minikube_ip=$( minikube -p "${MINIKUBE_PROFILE}" ip ) || die "can't find minikube ip. Is it started?"
  ping -c 1 "${minikube_ip}" || die "minikube is not responding to ping. Is it started?"
}

for configuration_profile in "$@"; do configure "$configuration_profile"; done
