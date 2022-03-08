#!/bin/bash -e

########
# Run Fleet Shard locally in dev mode
#
########

args=$@
shift $#

script_dir_path=$(dirname "${BASH_SOURCE[0]}")

. ${script_dir_path}/common.sh

# stop first any remaining process which could conflict
${script_dir_path}/local-quarkus-service-stop.sh 'shard-operator'

configure_cluster_started

export INGRESS_OVERRIDE_HOSTNAME=${cluster_ip}

mvn \
  -Dminikubeip=${cluster_ip} \
  -Dquarkus.http.port=1337 \
  -Pminikube \
  -f "${root_dir}/shard-operator/pom.xml" \
  clean compile quarkus:dev $args
