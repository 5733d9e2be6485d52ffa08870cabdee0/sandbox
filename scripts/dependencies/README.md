# Data Plane shard operator dependencies 

The shard operator requires serverless [1] and servicemesh [2] operators (servicemesh requires two more operators, see the documentation).

They can be installed, along with their additional configuration resources, by running the script `install_dependencies.sh`.
The operators and their configuration resources can be removed by running `uninstall_dependencies.sh`.

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
[1] https://docs.openshift.com/container-platform/4.11/serverless/install/install-serverless-operator.html

[2] https://docs.openshift.com/container-platform/4.11/service_mesh/v2x/installing-ossm.html
