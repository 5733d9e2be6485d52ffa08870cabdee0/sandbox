#!/bin/bash

########
# Run Fleet Manager locally in dev mode
#
# Env vars:
# - MANAGED_CONNECTORS_NAMESPACE_ID: namespace where managed connectors will be deployed (required only if MC actions are used, default="empty")
# - MANAGED_CONNECTORS_CONTROL_PLANE_URL: endpoint of the MC Control plane. (required only if MC actions are used, default="empty")
# - MANAGED_CONNECTORS_AUTH_OFFLINE_TOKEN: Red Hat account offline token used by the fleet manager to authenticate to managed connectors (required)
########

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

export MANAGED_KAFKA_INSTANCE_NAME=rhose-local-development

. "${SCRIPT_DIR_PATH}/configure.sh" kafka minikube-started managed-connectors

bootstrap_server_host=$( getManagedKafkaBootstrapServerHost )
admin_client_id=$( getManagedKafkaAdminSAClientId )
admin_client_secret=$( getManagedKafkaAdminSAClientSecret )
ops_client_id=$( getManagedKafkaOpsSAClientId )
ops_client_secret=$( getManagedKafkaOpsSAClientSecret )
mc_client_id=$( getManagedKafkaMcSAClientId )
mc_client_secret=$( getManagedKafkaMcSAClientSecret )

export KAFKA_CLIENT_ID=${ops_client_id}
export KAFKA_CLIENT_SECRET=${ops_client_secret}
export MANAGED_CONNECTORS_KAFKA_CLIENT_ID=${mc_client_id}
export MANAGED_CONNECTORS_KAFKA_CLIENT_SECRET=${mc_client_secret}

rm -rf ${LOCAL_ENV_FILE}
echo "MANAGER_URL=http://localhost:8080" >> ${LOCAL_ENV_FILE}
echo "KEYCLOAK_URL=http://$(minikube -p "${MINIKUBE_PROFILE}" ip):30007" >> ${LOCAL_ENV_FILE}

# Note: '-Dkafka.*' properties are not required but setting them prevents annoying warning messages in the console
mvn \
  -Devent-bridge.kafka.bootstrap.servers=${bootstrap_server_host} \
  -Devent-bridge.kafka.client.id=${ops_client_id} \
  -Devent-bridge.kafka.client.secret=${ops_client_secret} \
  -Devent-bridge.kafka.security.protocol=SASL_SSL \
  -Devent-bridge.rhoas.instance-api.host=https://admin-server-${bootstrap_server_host}/rest \
  -Devent-bridge.rhoas.mgmt-api.host=https://api.openshift.com \
  -Devent-bridge.rhoas.sso.mas.auth-server-url=https://sso.redhat.com/auth/realms/redhat-external \
  -Devent-bridge.rhoas.sso.mas.client-id=${admin_client_id} \
  -Devent-bridge.rhoas.sso.mas.client-secret=${admin_client_secret} \
  -Dminikubeip=${MINIKUBE_IP} \
  -Drhoas.ops-account.client-id=${ops_client_id} \
  -Dmanaged-connectors.namespace.id=${MANAGED_CONNECTORS_NAMESPACE_ID} \
  -Dmanaged-connectors.kafka.bootstrap.servers=${bootstrap_server_host} \
  -Dmanaged-connectors.kafka.client.id=${mc_client_id} \
  -Dmanaged-connectors.kafka.client.secret=${mc_client_secret} \
  -Dmanaged-connectors.kafka.security.protocol=SASL_SSL \
  -Dmanaged-connectors.services.url=${MANAGED_CONNECTORS_CONTROL_PLANE_URL} \
  -Dmanaged-connectors.auth.server-url=https://sso.redhat.com/auth/realms/redhat-external \
  -Dmanaged-connectors.auth.token-path=protocol/openid-connect/token \
  -Dmanaged-connectors.auth.client-id=cloud-services \
  -Dmanaged-connectors.auth.offline-token=${MANAGED_CONNECTORS_AUTH_OFFLINE_TOKEN} \
  \
  -Dquarkus.devservices.enabled=false \
  -Dkafka.client.id=${mc_client_id} \
  -Dkafka.client.secret=${mc_client_secret} \
  -Dkafka.security.protocol=SASL_SSL \
  -Dkafka.sasl.jaas.config="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${mc_client_id}\" password=\"${mc_client_secret}\";" \
  -Dkafka.sasl.mechanism=PLAIN \
  \
  -f "$( dirname "$0" )/../../manager/pom.xml" \
  clean compile quarkus:dev $@
