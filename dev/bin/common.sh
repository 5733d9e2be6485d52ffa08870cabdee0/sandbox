#!/bin/bash

configure_script_dir_path=`dirname "${BASH_SOURCE[0]}"`

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

# list of required tools
required_tools="
docker-compose
jq
kubectl
kustomize
mvn
ping
"

# check if required tools are installed
for tool in $required_tools; do
  which "$tool" &> /dev/null || die "required tool \"$tool\" is missing"
done

# useful paths
root_dir=`realpath ${configure_script_dir_path}/../..`
dev_dir="${root_dir}/dev"
local_deploy_dir="${dev_dir}/_deploy"
dev_config_dir="${dev_dir}/config"
dev_bin_dir="${dev_dir}/bin"
dev_logs_dir="${dev_dir}/logs"
dev_docker_compose_dir="${dev_dir}/docker-compose"
credentials_dir="${dev_bin_dir}/credentials"
kustomize_deploy_dir="${local_deploy_dir}/kustomize"
local_env_file="${local_deploy_dir}/local_env"
local_config_file="${dev_config_dir}/localconfig"
kustomize_dir="${root_dir}/kustomize"
project_env_file="${root_dir}/.env"

function configure_kafka {
  retrieve_full_env
  check_required_variable "MANAGED_KAFKA_INSTANCE_NAME"
  manager_kafka_credentials_file="${credentials_dir}/${MANAGED_KAFKA_INSTANCE_NAME}.json"
  if [[ ! -f ${manager_kafka_credentials_file} ]] ; then
    echo "${manager_kafka_credentials_file}. Please follow the 'Managed Kafka instance setup' dev/README.md instructions to setup them."
    exit
  fi
  admin_sa_name="${MANAGED_KAFKA_INSTANCE_NAME}-admin"
  admin_sa_credentials_file="${credentials_dir}/${admin_sa_name}.json"
  if [[ ! -f ${admin_sa_credentials_file} ]] ; then
    echo "${admin_sa_credentials_file}. Please follow the 'Managed Kafka instance setup' dev/README.md instructions to setup them."
    exit
  fi
  ops_sa_name="${MANAGED_KAFKA_INSTANCE_NAME}-ops"
  ops_sa_credentials_file="${credentials_dir}/${ops_sa_name}.json"
  if [[ ! -f ${ops_sa_credentials_file} ]] ; then
    echo "${ops_sa_credentials_file}. Please follow the 'Managed Kafka instance setup' dev/README.md instructions to setup them."
    exit
  fi
  mc_sa_name="${MANAGED_KAFKA_INSTANCE_NAME}-mc"
  mc_sa_credentials_file="${credentials_dir}/${mc_sa_name}.json"
  if [[ ! -f ${mc_sa_credentials_file} ]] ; then
    echo "${mc_sa_credentials_file}. Please follow the 'Managed Kafka instance setup' dev/README.md instructions to setup them."
    exit
  fi
}

function configure_managed_connectors {
  retrieve_full_env
  check_required_variable "OPENSHIFT_OFFLINE_TOKEN"
  managed_connectors_namespace_id="${MANAGED_CONNECTORS_NAMESPACE_ID:-empty}"
}

function configure_cluster {
  retrieve_full_env
  cluster_type=${CLUSTER_TYPE:-minikube}
  if [ "${cluster_type}" = 'minikube' ]; then
    configure_minikube
  elif [ "${cluster_type}" = 'kind' ]; then
    configure_kind
  fi
}

function configure_minikube {
  retrieve_full_env
  minikube_profile="${MINIKUBE_PROFILE:-minikube}"
  minikube_cpus="${MINIKUBE_CPUS:-4}"
  minikube_memory="${MINIKUBE_MEMORY:-8192}"
  minikube_kubernetes_version="${MINIKUBE_KUBERNETES_VERSION}"
  minikube_driver="${MINIKUBE_DRIVER}"
  minikube_container_runtime="${MINIKUBE_CONTAINER_RUNTIME}"
}

function configure_kind {
  retrieve_full_env
  kind_name="${KIND_NAME:-kind}"
  kind_config_file="${KIND_CONFIG_FILE:-"${dev_config_dir}/kind/config.yaml"}"
  kind_container_engine="${KIND_CONTAINER_ENGINE}"
  kind_network="${KIND_NETWORK}"
  kind_kubernetes_version="${KIND_KUBERNETES_VERSION}"
  
}

function configure_cluster_started {
  retrieve_full_env
  configure_cluster
  cluster_ip=${CLUSTER_IP}
  ping -c 1 ${cluster_ip} &> /dev/null || die "cluster is not responding to ping. Is it started?"
}

function configure_keycloak {
  retrieve_full_env
  keycloak_url=${KEYCLOAK_URL}
  is_keycloak_internal_url=${IS_KEYCLOAK_INTERNAL_URL}
  keycloak_username=${KEYCLOAK_USERNAME:-webhook-robot-1}
  keycloak_password=${KEYCLOAK_PASSWORD:-therobot}
  keycloak_client_id=${KEYCLOAK_CLIENT_ID:-event-bridge}
  keycloak_client_secret=${KEYCLOAK_CLIENT_SECRET:-secret}
  keycloak_token_offline_access=${KEYCLOAK_TOKEN_OFFLINE_ACCESS:-true}
}

function configure_manager {
  retrieve_full_env
  manager_url=${MANAGER_URL}
}

function configure_images {
  retrieve_full_env
  fleet_manager_container_name=${FLEET_MANAGER_CONTAINER_NAME:-'openbridge/manager:latest'}
  fleet_shard_container_name=${FLEET_SHARD_CONTAINER_NAME:-'openbridge/shard-operator:latest'}
  executor_container_name=${EXECUTOR_CONTAINER_NAME:-'openbridge/executor:latest'}
  ingress_container_name=${INGRESS_CONTAINER_NAME:-'openbridge/ingress:latest'}
}

function get_keycloak_access_token {
  configure_keycloak
  local keycloak_cmd_data="username=${keycloak_username}&password=${keycloak_password}&grant_type=password"
  if [ "${keycloak_token_offline_access}" = "true" ]; then
    keycloak_cmd_data="${keycloak_cmd_data}&scope=offline_access"
  fi
  local keycloak_cmd="curl --insecure -X POST ${keycloak_url}/auth/realms/event-bridge-fm/protocol/openid-connect/token --user ${keycloak_client_id}:${keycloak_client_secret} -d ${keycloak_cmd_data}"
  if [ "${is_keycloak_internal_url}" = 'true' ]; then
    echo "$(kubectl exec -n keycloak deployment/keycloak -- ${keycloak_cmd} -H 'content-type: application/x-www-form-urlencoded' | jq --raw-output '.access_token')"
  else
    echo "$(${keycloak_cmd} -H 'content-type: application/x-www-form-urlencoded' | jq --raw-output '.access_token')"
  fi
}

function get_sso_access_token {
  echo "$(curl -s --insecure -X POST ${SSO_REDHAT_URL}/auth/realms/redhat-external/protocol/openid-connect/token --header 'Content-Type: application/x-www-form-urlencoded' --data-urlencode 'client_id=cloud-services' --data-urlencode 'grant_type=refresh_token' --data-urlencode "refresh_token=${OPENSHIFT_OFFLINE_TOKEN}" | jq --raw-output '.access_token')"
}

function get_json_value {
  echo "$( jq -r "$1" "$2" )"
}

function get_sa_client_id {
  echo "$( get_json_value '.clientID' "$1" )"
}

function get_sa_client_secret {
  echo "$( get_json_value '.clientSecret' "$1" )"
}

function get_managed_kafka_bootstrap_server {
  configure_kafka
  echo "$( get_json_value '.bootstrap_server_host' "${manager_kafka_credentials_file}" )"
}

function get_managed_kafka_admin_sa_client_id {
  configure_kafka
  echo "$( get_sa_client_id "${admin_sa_credentials_file}" )"
}

function get_managed_kafka_admin_sa_client_secret {
  configure_kafka
  echo "$( get_sa_client_secret "${admin_sa_credentials_file}" )"
}

function get_managed_kafka_ops_sa_client_id {
  configure_kafka
  echo "$( get_sa_client_id "${ops_sa_credentials_file}" )"
}

function get_managed_kafka_ops_sa_client_secret {
  configure_kafka
  echo "$( get_sa_client_secret "${ops_sa_credentials_file}" )"
}

function get_managed_kafka_mc_sa_client_id {
  configure_kafka
  echo "$( get_sa_client_id "${mc_sa_credentials_file}" )"
}

function get_managed_kafka_mc_sa_client_secret {
  configure_kafka
  echo "$( get_sa_client_secret "${mc_sa_credentials_file}" )"
}

function retrieve_full_env {
  if [ -f ${project_env_file} ]; then
    . "${project_env_file}"
  fi

  # load local config if found
  if [ -f "${local_config_file}" ]; then
    . "${local_config_file}"
  fi
  # load local env if found
  if [ -f "${local_env_file}" ]; then
    . "${local_env_file}"
  fi
}

function write_local_env {
  if [ ! -d $(dirname ${local_env_file}) ]; then
    mkdir -p "$(dirname ${local_env_file})"
  fi
  if [ ! -f ${local_env_file} ]; then
    touch ${local_env_file}
  fi
  if cat ${local_env_file} | grep $1 &> /dev/null; then
    sed -i "s|.*$1=.*|$1=$2|g" ${local_env_file}
  else
    echo "$1=$2" >> ${local_env_file}
  fi
}

function remove_local_env {
  if [ -f ${local_env_file} ]; then
    sed -i "/$1/d" ${local_env_file}
  fi
}

function reset_local_env {
  rm -rf ${local_env_file}
}

function kill_process_with_name {
    process_name=$1
    echo "Kill all process ids which match the name ${process_name}"
    for process_id in $(ps -ax | grep "${process_name}" | grep -v grep | awk '{print $1}')
    do
        echo "Kill current running process with id ${process_id}"
        kill -9 ${process_id}
    done
}

######################
# migration part for config files
# can be removed after some time
mkdir -p "${dev_config_dir}"
if [ -f "${dev_bin_dir}/localconfig" ]; then
  mv "${dev_bin_dir}/localconfig" "${local_config_file}"
fi
if [ -f "${dev_bin_dir}/interaction/environment" ]; then
  mv "${dev_bin_dir}/interaction/environment" "${dev_config_dir}/interactionconfig"
fi
######################