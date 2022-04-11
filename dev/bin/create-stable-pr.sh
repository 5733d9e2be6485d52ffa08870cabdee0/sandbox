#!/bin/sh

COMMIT_HASH=${1}
COMMIT_DESCRIPTION=$(git log --pretty=format:"%h %s" -1 $COMMIT_HASH)

echo "DEV COMMIT: $COMMIT_HASH"
echo "$COMMIT_DESCRIPTION"

export MANAGER_NEW_TAG=$(git show $COMMIT_HASH:kustomize/overlays/prod/kustomization.yaml | yq .images[0].newTag)
export OPERATOR_NEW_TAG=$(git show $COMMIT_HASH:kustomize/overlays/prod/kustomization.yaml | yq .images[1].newTag)

echo "MANAGER_NEW_TAG: $MANAGER_NEW_TAG"
echo "OPERATOR_NEW_TAG: $OPERATOR_NEW_TAG"

export EXECUTOR_IMAGE=$(git show $COMMIT_HASH:kustomize/overlays/prod/shard/patches/deploy-config.yaml | yq .data.EVENT_BRIDGE_EXECUTOR_IMAGE)
export INGRESS_IMAGE=$(git show $COMMIT_HASH:kustomize/overlays/prod/shard/patches/deploy-config.yaml | yq .data.EVENT_BRIDGE_INGRESS_IMAGE)

echo "EXECUTOR_IMAGE: $EXECUTOR_IMAGE"
echo "INGRESS_IMAGE: $INGRESS_IMAGE"

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

KUSTOMIZE_DIR="${SCRIPT_DIR_PATH}/../../kustomize"

yq -i '
  .images[0].newTag = strenv(MANAGER_NEW_TAG) |
  .images[1].newTag = strenv(OPERATOR_NEW_TAG)
' $KUSTOMIZE_DIR/overlays/stable/kustomization.yaml

yq -i '
  .data.EVENT_BRIDGE_EXECUTOR_IMAGE = strenv(EXECUTOR_IMAGE) |
  .data.EVENT_BRIDGE_INGRESS_IMAGE = strenv(INGRESS_IMAGE)
' $KUSTOMIZE_DIR/overlays/stable/shard/patches/deploy-config.yaml


git commit -am "To Stable: $COMMIT_DESCRIPTION"





