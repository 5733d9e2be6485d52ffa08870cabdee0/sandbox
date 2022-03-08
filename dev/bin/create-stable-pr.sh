#!/bin/sh

if ! command -v yq &> /dev/null
then
    echo "yq could not be found, install it from https://github.com/mikefarah/yq"
    exit
fi

if (( $# == 0 )); then
    echo "Usage: ./create-stable-br <GIT_DEV_DEPLOY_HASH>"
    exit
fi

commit_hash=${1}
commit_description=$(git log --pretty=format:"%h %s" -1 ${commit_hash})
shift

. $(dirname "${BASH_SOURCE[0]}")/common.sh

echo "DEV COMMIT: $commit_hash"
echo "${commit_description}"

manager_new_tag=$(git show ${commit_hash}:kustomize/overlays/prod/kustomization.yaml | yq .images[0].newTag)
operator_new_tag=$(git show ${commit_hash}:kustomize/overlays/prod/kustomization.yaml | yq .images[1].newTag)

echo "manager_new_tag: ${manager_new_tag}"
echo "operator_new_tag: ${operator_new_tag}"

executor_image=$(git show ${commit_hash}:kustomize/overlays/prod/shard/patches/deploy-config.yaml | yq .data.EVENT_BRIDGE_executor_image)
ingress_image=$(git show ${commit_hash}:kustomize/overlays/prod/shard/patches/deploy-config.yaml | yq .data.EVENT_BRIDGE_ingress_image)

echo "executor_image: ${executor_image}"
echo "ingress_image: ${ingress_image}"

yq -i '
  .images[0].newTag = strenv(manager_new_tag) |
  .images[1].newTag = strenv(operator_new_tag)
' ${kustomize_dir}/overlays/stable/kustomization.yaml

yq -i '
  .data.EVENT_BRIDGE_executor_image = strenv(executor_image) |
  .data.EVENT_BRIDGE_ingress_image = strenv(ingress_image)
' ${kustomize_dir}/overlays/stable/shard/patches/deploy-config.yaml


git commit -am "To Stable: $commit_description"





