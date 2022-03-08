#!/bin/bash -e

########
# Build docker images and save them to clusters registry
# This will also update the kustomize deploy dir
# Kustomize directory can be changed via first arg of the script
########

deploy_dir=$1
if [ -n "$deploy_dir" ]; then
    shift
fi

. $(dirname "${BASH_SOURCE[0]}")/common.sh

if [ -z ${deploy_dir} ]; then
    deploy_dir=${kustomize_deploy_dir}
fi

configure_cluster_started
configure_images

stat "${root_dir}" &> /dev/null || die "Can't access repository root"

image_tag=
if which uuidgen &> /dev/null; then
    image_tag=$(uuidgen)
else
    image_tag=$RANDOM
fi

fleet_manager_container_name=$(echo ${fleet_manager_container_name} | sed -E "s|(.*):.*|\1:${image_tag}|")
fleet_shard_container_name=$(echo ${fleet_shard_container_name} | sed -E "s|(.*):.*|\1:${image_tag}|")
executor_container_name=$(echo ${executor_container_name} | sed -E "s|(.*):.*|\1:${image_tag}|")
ingress_container_name=$(echo ${ingress_container_name} | sed -E "s|(.*):.*|\1:${image_tag}|")

echo 'Build projects'
mvn clean install -f ${root_dir}/pom.xml -Dquickly

cluster_load_command=
container_engine='docker'
internal_image_prefix=
if [ "${cluster_type}" = 'minikube' ]; then
    echo 'Build&Load images into minikube'
    cluster_load_command='minikube image load'
elif [ "${cluster_type}" = 'kind' ]; then
    echo 'Build&Load images into kind'
    cluster_load_command='kind load docker-image'
else
    echo "Unsupported cluster ${cluster_type}. Please use `minikube` or `kind`."
fi

${container_engine} build -f docker/Dockerfile.jvm -t ${fleet_manager_container_name} manager/
${container_engine} build -f docker/Dockerfile.jvm -t ${fleet_shard_container_name} shard-operator/
${container_engine} build -f docker/Dockerfile.jvm -t ${executor_container_name} executor/
${container_engine} build -f docker/Dockerfile.jvm -t ${ingress_container_name} ingress/

set -x

${cluster_load_command} ${fleet_manager_container_name}
${cluster_load_command} ${fleet_shard_container_name}
${cluster_load_command} ${executor_container_name}
${cluster_load_command} ${ingress_container_name}

set +x

write_local_env 'FLEET_MANAGER_CONTAINER_NAME' ${fleet_manager_container_name}
write_local_env 'FLEET_SHARD_CONTAINER_NAME' ${fleet_shard_container_name}
write_local_env 'EXECUTOR_CONTAINER_NAME' ${executor_container_name}
write_local_env 'INGRESS_CONTAINER_NAME' ${ingress_container_name}