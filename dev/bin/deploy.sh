#!/bin/bash -e

usage() {
    echo 'Usage: deploy.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -l                  Use images built locally (they will be built automatically)'
    echo '  -s                  Start the cluster'
    echo '  -a                  Deploy all resources on the cluster'
    echo
    echo 'Examples:'
    echo '  # Simple deployment. Only dev image, aka ingress and executor will be deployed to cluster'
    echo '  sh deploy.sh'
    echo
    echo '  # Start Kind cluster and deploy everything on it with local build of images'
    echo '  sh deploy.sh -a -l -s'
}

start_cluster=false
deploy_all=false
use_local_images=false

while getopts "c:lskah" i
do
    case "$i"
    in
        s) start_cluster=true ;;
        a) deploy_all=true ;;
        l) use_local_images=true ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

. $(dirname "${BASH_SOURCE[0]}")/common.sh

echo "---------------- Prepare deploy dir ----------------"
${dev_bin_dir}/cluster-prepare-resources.sh

echo "---------------- Setup Managed Kafka and Managed Connectors ----------------"
configure_kafka
configure_managed_connectors

if [ "${start_cluster}" = "true" ]; then
    echo "---------------- Starting Cluster ----------------"
    ${dev_bin_dir}/cluster-start.sh
fi
configure_cluster_started

if [ "${use_local_images}" = 'true' ]; then
    echo "---------------- Building local images and load them into ${cluster_type} cluster ----------------"
    ${dev_bin_dir}/cluster-load-containers.sh
fi

echo "---------------- Deploy keycloak on ${cluster_type} cluster ----------------"
${dev_bin_dir}/cluster-deploy-keycloak.sh
configure_keycloak

echo "---------------- Deploy Prometheus CRDs on ${cluster_type} cluster ----------------"
${dev_bin_dir}/cluster-deploy-prometheus-crd.sh


if [ "${deploy_all}" = 'true' ]; then
    write_local_env 'DEV_ENVIRONMENT' 'false'

    echo "---------------- Deploying ALL resources into ${cluster_type} cluster ----------------"
    ${dev_bin_dir}/cluster-deploy-all-cluster-resources.sh

    echo "---------------- Wait for manager and shard operator to start ----------------"
    kubectl wait --for=condition=available --timeout=240s deployment/event-bridge-shard-operator -n event-bridge-operator
    kubectl wait --for=condition=available --timeout=240s deployment/event-bridge -n event-bridge-manager
else
    write_local_env 'DEV_ENVIRONMENT' 'true'

    echo "---------------- Deploy dev resources to ${cluster_type} cluster ----------------"
    mkdir -p ${dev_logs_dir}

    # shouldn't we deploy those in cluster ?
    echo "---------------- Start docker services with docker-compose ----------------"
    echo "You can check logs with command:"
    echo
    printf "\tdocker-compose -f ${dev_docker_compose_dir}/docker-compose.yml logs\n" 
    echo
    docker-compose -f ${dev_docker_compose_dir}/docker-compose.yml up -d

    echo "---------------- Start local manager ----------------"
    echo "You can check logs with command:"
    echo
    printf "\ttail -f ${dev_logs_dir}/manager.log\n" 
    echo
    ${dev_bin_dir}/local-manager-start.sh &> ${dev_logs_dir}/manager.log &
    echo "Manager process started"

    echo "---------------- Start local shard operator ----------------"
    echo "You can check logs with command:"
    echo
    printf "\ttail -f ${dev_logs_dir}/shard-operator.log\n"
    echo
    ${dev_bin_dir}/local-shard-start.sh &> ${dev_logs_dir}/shard-operator.log &
    echo "Shard operator process started"

    echo "---------------- Wait for manager and shard operator to start ----------------"
    timeout 120 bash -c "while ! grep -a 'Listening on: http://localhost:8080' ${dev_logs_dir}/manager.log &> /dev/null; do sleep 5; done"
    echo 'Manager is up'
    timeout 120 bash -c "while ! grep -a 'Listening on: http://localhost:1337' ${dev_logs_dir}/shard-operator.log &> /dev/null; do sleep 5; done"
    echo 'Shard operator is up'
fi
