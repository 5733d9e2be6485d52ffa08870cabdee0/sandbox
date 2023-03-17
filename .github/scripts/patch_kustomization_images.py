import sys
import re
import yaml

def patch_fleet_manager(image: str):

    # Extract only the image tag
    tag = image.split("fleet-manager:")[1]

    # prod overlay
    with open("kustomize/base-openshift/kustomization.yaml", "r") as stream:
        try:
            base_kustomization = yaml.full_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            sys.exit(1)

    manager = next(filter(lambda x: x['name'] == 'event-bridge-manager', base_kustomization['images']))
    manager['newTag'] = tag

    with open('kustomize/base-openshift/kustomization.yaml', 'w') as outfile:
        yaml.dump(base_kustomization, outfile)

    # ci overlay
    with open("kustomize/overlays/ci/kustomization.yaml", "r") as stream:
        try:
            ci_kustomization = yaml.full_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            sys.exit(1)

    manager = next(filter(lambda x: x['name'] == 'event-bridge-manager', ci_kustomization['images']))
    manager['newTag'] = tag

    with open('kustomize/overlays/ci/kustomization.yaml', 'w') as outfile:
        yaml.dump(ci_kustomization, outfile)

def patch_fleet_shard(image: str):

    # Extract only the image tag
    tag = image.split("fleet-shard:")[1]

    # base overlay
    with open("kustomize/base-openshift/kustomization.yaml", "r") as stream:
        try:
            base_kustomization = yaml.full_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            sys.exit(1)

    shard = next(filter(lambda x: x['name'] == 'event-bridge-shard-operator', base_kustomization['images']))
    shard['newTag'] = tag

    with open('kustomize/base-openshift/kustomization.yaml', 'w') as outfile:
        yaml.dump(base_kustomization, outfile)

    # prod overlay
    with open("kustomize/overlays/prod/kustomization.yaml", "r") as stream:
        try:
            prod_kustomization = yaml.full_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            sys.exit(1)

    shard = next(filter(lambda x: x['name'] == 'event-bridge-shard-operator', prod_kustomization['images']))
    shard['newTag'] = tag

    with open('kustomize/overlays/prod/kustomization.yaml', 'w') as outfile:
        yaml.dump(prod_kustomization, outfile)

    # ci overlay
    with open("kustomize/overlays/ci/kustomization.yaml", "r") as stream:
        try:
            ci_kustomization = yaml.full_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            sys.exit(1)

    shard = next(filter(lambda x: x['name'] == 'event-bridge-shard-operator', ci_kustomization['images']))
    shard['newTag'] = tag.replace("ocp-", "k8s-")

    with open('kustomize/overlays/ci/kustomization.yaml', 'w') as outfile:
        yaml.dump(ci_kustomization, outfile)

def patch_executor(image: str):

    # base
    with open("kustomize/base-openshift/shard/patches/deploy-config.yaml", "r") as stream:
        try:
            shard_patch = yaml.full_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            sys.exit(1)

    shard_patch['data']['EVENT_BRIDGE_EXECUTOR_IMAGE'] = image

    with open('kustomize/base-openshift/shard/patches/deploy-config.yaml', 'w') as outfile:
        yaml.dump(shard_patch, outfile)

    # CI
    with open("kustomize/overlays/ci/shard/patches/deploy-config.yaml", "r") as stream:
        try:
            shard_patch = yaml.full_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            sys.exit(1)

    shard_patch['data']['EVENT_BRIDGE_EXECUTOR_IMAGE'] = image

    with open('kustomize/overlays/ci/shard/patches/deploy-config.yaml', 'w') as outfile:
        yaml.dump(shard_patch, outfile)

    # prod overlay
    with open("kustomize/overlays/prod/patches/deploy-config.yaml", "r") as stream:
        try:
            shard_patch = yaml.full_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            sys.exit(1)

    shard_patch['data']['EVENT_BRIDGE_EXECUTOR_IMAGE'] = image

    with open('kustomize/overlays/prod/patches/deploy-config.yaml', 'w') as outfile:
        yaml.dump(shard_patch, outfile)

if __name__ == "__main__":
    image = sys.argv[1]
    component = sys.argv[2]

    if (component == "fleet_manager"):
        patch_fleet_manager(image)
    elif (component == "fleet_shard"):
        patch_fleet_shard(image)
    elif (component == "executor"):
        patch_executor(image)
    else:
        raise Exception("Not a valid component option to patch.")
