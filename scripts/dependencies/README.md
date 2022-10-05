# Data Plane shard operator dependencies 

The shard operator requires serverless [1] and servicemesh [2] operators (servicemesh requires two more operators, see the documentation).

They can be installed, along with their additional configuration resources, by running the script `install_dependencies.sh`.
The operators and their configuration resources can be removed by running `uninstall_dependencies.sh`.

OpenShift Serverless is currently installed using the midstream operator. That is identical to the fully released product, except:
- (pro) It is easier and faster to consume bug fixes
- Midstream operator and Knative images use non-productized base images
- (con) Serverless QA testing is done on the product operator, not the midstream one. However, as long as there is a
  matching product, the midstream operator should be almost identical to it.

## Prerequisites
- A compatible `oc` client binary on PATH
- Before running any of the scripts, the `oc` client needs to be authenticated into the target cluster (`oc login`). 

## Installation
```bash
oc login --token=... --server=...
./install_dependencies.sh
```

## Uninstallation
```bash
oc login --token=... --server=...
./uninstall_dependencies.sh
```

## References
[1] https://github.com/openshift-knative/serverless-operator/blob/main/docs/install-midstream.md

[2] https://docs.openshift.com/container-platform/4.11/service_mesh/v2x/installing-ossm.html
