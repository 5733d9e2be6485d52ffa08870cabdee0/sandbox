#!/bin/bash -e

########
# Run Fleet Manager locally in dev mode
#
# Env vars:
# - MANAGED_CONNECTORS_NAMESPACE_ID: namespace where managed connectors will be deployed (required only if MC actions are used, default="empty")
# - MANAGED_CONNECTORS_CONTROL_PLANE_URL: endpoint of the MC Control plane. (required only if MC actions are used, default="empty")
# - OPENSHIFT_OFFLINE_TOKEN: Red Hat account offline token (required, get it at https://console.redhat.com/openshift/token)
########

args=$@
shift $#

script_dir_path=$(dirname "${BASH_SOURCE[0]}")

. ${script_dir_path}/common.sh

# stop first any remaining process which could conflict
${script_dir_path}/local-quarkus-service-stop.sh 'manager'

configure_kafka
configure_cluster_started
configure_managed_connectors

bootstrap_server_host=$( get_managed_kafka_bootstrap_server )
admin_client_id=$( get_managed_kafka_admin_sa_client_id )
admin_client_secret=$( get_managed_kafka_admin_sa_client_secret )
ops_client_id=$( get_managed_kafka_ops_sa_client_id )
ops_client_secret=$( get_managed_kafka_ops_sa_client_secret )
mc_client_id=$( get_managed_kafka_mc_sa_client_id )
mc_client_secret=$( get_managed_kafka_mc_sa_client_secret )

export KAFKA_CLIENT_ID=${ops_client_id}
export KAFKA_CLIENT_SECRET=${ops_client_secret}
export MANAGED_CONNECTORS_KAFKA_CLIENT_ID=${mc_client_id}
export MANAGED_CONNECTORS_KAFKA_CLIENT_SECRET=${mc_client_secret}

write_local_env 'MANAGER_URL' "http://localhost:8080"

# Note: '-Dkafka.*' properties are not required but setting them prevents annoying warning messages in the console
mvn \
  -Devent-bridge.kafka.bootstrap.servers=${bootstrap_server_host} \
  -Devent-bridge.kafka.client.id=${ops_client_id} \
  -Devent-bridge.kafka.client.secret=${ops_client_secret} \
  -Devent-bridge.kafka.security.protocol=SASL_SSL \
  -Devent-bridge.rhoas.instance-api.host=https://admin-server-${bootstrap_server_host}/rest \
  -Devent-bridge.rhoas.mgmt-api.host=https://api.openshift.com \
  -Devent-bridge.rhoas.sso.mas.auth-server-url=https://identity.api.openshift.com/auth/realms/rhoas \
  -Devent-bridge.rhoas.sso.mas.client-id=${admin_client_id} \
  -Devent-bridge.rhoas.sso.mas.client-secret=${admin_client_secret} \
  -Dminikubeip=${cluster_ip} \
  -Drhoas.ops-account.client-id=${ops_client_id} \
  -Dmanaged-connectors.namespace.id=${managed_connectors_namespace_id} \
  -Dmanaged-connectors.kafka.bootstrap.servers=${bootstrap_server_host} \
  -Dmanaged-connectors.kafka.client.id=${mc_client_id} \
  -Dmanaged-connectors.kafka.client.secret=${mc_client_secret} \
  -Dmanaged-connectors.kafka.security.protocol=SASL_SSL \
  -Dmanaged-connectors.services.url=${MANAGED_CONNECTORS_CONTROL_PLANE_URL} \
  -Dmanaged-connectors.auth.server-url=https://sso.redhat.com/auth/realms/redhat-external \
  -Dmanaged-connectors.auth.token-path=protocol/openid-connect/token \
  -Dmanaged-connectors.auth.client-id=cloud-services \
  -Dmanaged-connectors.auth.offline-token=${OPENSHIFT_OFFLINE_TOKEN} \
  \
  -Dquarkus.devservices.enabled=false \
  -Dkafka.client.id=${mc_client_id} \
  -Dkafka.client.secret=${mc_client_secret} \
  -Dkafka.security.protocol=SASL_SSL \
  -Dkafka.sasl.jaas.config="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${mc_client_id}\" password=\"${mc_client_secret}\";" \
  -Dkafka.sasl.mechanism=PLAIN \
  \
  -Dquarkus.http.port=8080 \
  -f "$( dirname "$0" )/../../manager/pom.xml" \
  clean compile quarkus:dev $args
