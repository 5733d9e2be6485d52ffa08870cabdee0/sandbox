#!/bin/bash -e

########
# Prepare all resources with kustomize.
# Kustomize output directory can be changed via first arg of the script
########

deploy_dir=$1
if [ -n "$deploy_dir" ]; then
    shift
fi

. $(dirname "${BASH_SOURCE[0]}")/common.sh

if [ -z ${deploy_dir} ]; then
    deploy_dir=${kustomize_deploy_dir}
fi

echo "Remove kustomize deploy dir"
rm -rf ${kustomize_deploy_dir}

echo "Copy kustomize dir to deploy folder"
mkdir -p ${kustomize_deploy_dir}/overlays
cp -r ${kustomize_dir}/base ${kustomize_deploy_dir}
cp -r ${kustomize_dir}/overlays/minikube ${kustomize_deploy_dir}/overlays