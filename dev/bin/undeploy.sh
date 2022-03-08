#!/bin/bash

usage() {
    echo 'Usage: undeploy.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -s                  Stop the cluster'
    echo
    echo 'Examples:'
    echo '  # Just deploy all resources from cluster'
    echo '  sh undeploy.sh'
    echo
    echo '  # Undeploy and stop cluster'
    echo '  sh undeploy.sh -s'
}

stop_cluster=false

while getopts "sh" i
do
    case "$i"
    in
        s) stop_cluster=true ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

. $(dirname "${BASH_SOURCE[0]}")/common.sh

configure_cluster_started

if [ "${DEV_ENVIRONMENT}" = 'true' ]; then
    echo "---------------- Kill local manager ----------------"
    ${dev_bin_dir}/local-quarkus-service-stop.sh 'manager'

    echo "---------------- Kill local shard operator ----------------"
    ${dev_bin_dir}/local-quarkus-service-stop.sh 'shard-operator'

    echo "---------------- Stop docker services with docker-compose ----------------"
    docker-compose -f ${dev_docker_compose_dir}/docker-compose.yml down
fi

echo "---------------- Undeploy resources ----------------"
kustomize build ${kustomize_deploy_dir}/overlays/minikube | kubectl delete -f - || true

echo "---------------- Remove remaining OB namespaces ----------------"
# This could be replaced by a read in manager of all OB instances running and wait for all of them to be deleted before deleting the resources
kubectl get ns | grep "Active" | awk -F " " '{print $1}' | grep "^ob-" | xargs kubectl delete ns || echo "nothing to delete"

if [ "${stop_cluster}" = "true" ]; then
    echo "---------------- Stopping Cluster ----------------"
    ${dev_bin_dir}/cluster-stop.sh

    echo "---------------- Remove deploy dir ----------------"
    rm -rf ${local_deploy_dir}
    echo "Deploy dir removed"
fi