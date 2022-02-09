#!/bin/bash

########
# Full setup of test Managed Kafka instance and service accounts.
# It is idempotent and can be run multiple times.
#
# Env vars:
# - MANAGED_KAFKA_INSTANCE_NAME: set the managed kafka instance name (required)
########

. "$( dirname "$0" )/configure.sh" kafka

# rhoas login
function rhoas_login {
  rhoas_logged_in=$( rhoas status kafka -o json &> /dev/null && echo -n "yes" || echo -n "no" )
  if [ "${rhoas_logged_in}" == "no" ]; then
    echo "rhoas not logged in. Please log in with your Red Hat account."
    rhoas login --print-sso-url
  fi
  rhoas_logged_in=$( rhoas status kafka -o json &> /dev/null && echo -n "yes" || echo -n "no" )
  if [ "${rhoas_logged_in}" == "no" ]; then
    die "rhoas login failure"
  else
    echo "rhoas logged in"
  fi
}

# create service accounts
function create_service_accounts {
  admin_sa_name="${MANAGED_KAFKA_INSTANCE_NAME}-admin"
  create_service_account "${admin_sa_name}"
  admin_sa_credentials_file="$( dirname "$0" )/credentials/${admin_sa_name}.json"
  admin_sa_id=$( jq -r '.clientID' "${admin_sa_credentials_file}" )
  rhoas kafka acl grant-admin -y --service-account "${admin_sa_id}"
  rhoas kafka acl create -y --user "${admin_sa_id}" --permission allow --operation create --topic all
  rhoas kafka acl create -y --user "${admin_sa_id}" --permission allow --operation delete --topic all
  echo "Admin account: ACLs created"

  ops_sa_name="${MANAGED_KAFKA_INSTANCE_NAME}-ops"
  create_service_account "${ops_sa_name}"
  ops_sa_credentials_file="$( dirname "$0" )/credentials/${ops_sa_name}.json"
  ops_sa_id=$( jq -r '.clientID' "${ops_sa_credentials_file}" )
  rhoas kafka acl create -y --user "${ops_sa_id}" --permission deny --operation alter --cluster
  rhoas kafka acl create -y --user "${ops_sa_id}" --permission deny --operation create --topic all
  rhoas kafka acl create -y --user "${ops_sa_id}" --permission deny --operation delete --topic all
  echo "Operational account: ACLs created"

  mc_sa_name="${MANAGED_KAFKA_INSTANCE_NAME}-mc"
  create_service_account "${mc_sa_name}"
  mc_sa_credentials_file="$( dirname "$0" )/credentials/${mc_sa_name}.json"
  mc_sa_id=$( jq -r '.clientID' "${mc_sa_credentials_file}" )
  rhoas kafka acl grant-admin -y --service-account "${mc_sa_id}"
  rhoas kafka acl create -y --user "${mc_sa_id}" --permission allow --operation all --group all
  rhoas kafka acl create -y --user "${mc_sa_id}" --permission allow --operation all --topic all
  rhoas kafka acl create -y --user "${mc_sa_id}" --permission allow --operation all --transactional-id all
  echo "Managed Connector account: ACLs created"
}

function create_service_account {
  mkdir -p "$( dirname "$0" )/credentials"

  sa_name="$1"
  sa_count=$( rhoas service-account list -o json | jq -rc ".items[] | select( .name == \"${sa_name}\" )" | wc -l )
  sa_credentials_file="$( dirname "$0" )/credentials/${sa_name}.json"

  if [ $sa_count -gt 1 ]; then
    die "ERROR: there are ${sa_count} service accounts named \"${sa_name}\""
  elif [ $sa_count -eq 0 ]; then
    echo "Creating service account named \"${sa_name}\"..."
    rhoas service-account create --output-file="${sa_credentials_file}" --file-format=json --overwrite --short-description="${sa_name}"
    echo "Created service account named \"${sa_name}\""
  else
    echo "Service account named \"${sa_name}\" found"
  fi

  sa_id=$( rhoas service-account list -o json | jq -rc ".items[] | select( .name == \"${sa_name}\" ) | .id" )
  [ -f "${sa_credentials_file}" ] || rhoas service-account reset-credentials --id "${sa_id}" --output-file="${sa_credentials_file}" -y
}

# create kafka instance
function create_kafka_instance {
  instance_count=$( rhoas kafka list --search "${MANAGED_KAFKA_INSTANCE_NAME}" -o json | jq -rc ".items[] | select( .name == \"${MANAGED_KAFKA_INSTANCE_NAME}\" )" | wc -l )
  if [ $instance_count -gt 1 ]; then
    die "ERROR: there are ${instance_count} instances named \"${MANAGED_KAFKA_INSTANCE_NAME}\""
  elif [ $instance_count -eq 0 ]; then
    echo "Creating Managed Kafka instance named \"${MANAGED_KAFKA_INSTANCE_NAME}\"..."
    rhoas kafka create --name "${MANAGED_KAFKA_INSTANCE_NAME}"
    echo "Created Managed Kafka instance named \"${MANAGED_KAFKA_INSTANCE_NAME}\""
  else
    echo "Managed Kafka instance named \"${MANAGED_KAFKA_INSTANCE_NAME}\" found"
  fi
}

# wait for kafka instance to become ready
function wait_kafka_ready {
  kafka_status=$( rhoas status kafka -o json | jq -rc '.kafka.status' )
  while [ "${kafka_status}" != "ready" ]; do
    echo "Waiting for Managed Kafka instance \"${MANAGED_KAFKA_INSTANCE_NAME}\" to become ready (current status \"${kafka_status}\")..."
    sleep 20s
    kafka_status=$( rhoas status kafka -o json | jq -rc '.kafka.status' )
  done
  echo "Managed Kafka instance \"${MANAGED_KAFKA_INSTANCE_NAME}\" is ${kafka_status}"
}

rhoas_login
create_service_accounts
create_kafka_instance
wait_kafka_ready
