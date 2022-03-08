# DEV

This guide uses [a set of BASH scripts](bin) to make the local development experience easier.

## Requirements

**IMPORTANT:** Unless specified, all the commands are supposed to be executed from the root of the repository

You will need the following installed locally on your machine to support local development:

* [Minikube v1.25.2](https://minikube.sigs.k8s.io/docs/start/) or [Kind v0.11.1](https://kind.sigs.k8s.io/docs/user/quick-start)
* [Docker Engine](https://docker.com)
  * The most recent version should be fine.
* [Docker Compose v1.29.2](https://github.com/docker/compose)
* [kustomize](https://kustomize.io/)
* [Maven v3.8.1](https://maven.apache.org/)
* [Java 11](https://adoptopenjdk.net/)
* [jq](https://stedolan.github.io/jq/)
* [curl](https://curl.se/) (or any other HTTP client)
* Many of us use and recommend [PostMan](https://postman.com) for testing our API instead of curl.

## Dev configuration

Configuration can be done via environment variables or via the `dev/config/localconfig` file.

If a file named `localconfig` is created in the [dev/config](config) folder, all its content is loaded before every script.  
This allows to define fixed local configurations once for all.  
Check the [localconfig-example](config/localconfig-example) file for an example of how to use it.

### Managed services configuration

Note: _A Red Hat account is required to log in to the remote services (e.g. Managed Kafka and Managed Connectors)_

Managed services require some **mandatory** variables:

| Name                                   | Description                                                                                                                                      |
|:---------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `MANAGED_CONNECTORS_NAMESPACE_ID` | ID of the namespace where Managed Connectors are deployed, required to use Managed Connectors based actions. Skip it if you don't need MC actions. |
| `MANAGED_CONNECTORS_CONTROL_PLANE_URL` | URL of the MC control plane, required to use Managed Connectors based actions. Skip it if you don't need MC actions. |
| `SSO_REDHAT_URL`                       | URL to Red Hat SSO. Default is           https://sso.redhat.com                                                            |
| `OPENSHIFT_OFFLINE_TOKEN`              | OpenShift offline token. To obtain it, go to https://console.redhat.com/openshift/token                                                          |

### Local cluster configuration

#### Minikube configuration for macOS users

It's important to set the `hyperkit` driver before starting the Minikube cluster due to [this bug](https://github.com/kubernetes/minikube/issues/7332)

Either export this env variable or add this to `localconfig` file (the latter method is suggested):

```bash 
MINIKUBE_DRIVER=hyperkit
```

#### Minikube configuration for Fedora users

With Fedora 34/35, there is an issue in spotting the coredns pod due to a cgroup issue with docker/podman.

One workaround is to use the `podman` driver and `cri-o` container runtime.

Either export this env variable or add this to `localconfig` file (the latter method is suggested):

```bash
MINIKUBE_DRIVER=podman
MINIKUBE_CONTAINER_RUNTIME=cri-o
```

#### Kind configuration for Fedora users

With Fedora 34/35, you will need to use the `calico` network in order for Kind to start correctly.

Just add this to your `localconfig` file:

```bash
KIND_CONFIG_FILE=dev/config/kind/config-calico.yaml
KIND_NETWORK=calico
```

## Deploy environment

To deploy the environment, you can use directly the `dev/bin/deploy.sh` script which will operate all for you.  
Or you can use separately the different scripts to just enable what you need.

To get an overview of the capabilities of the `dev/bin/deploy.sh` script, just run

```bash
./dev/bin/deploy.sh -h
```

Some of the scripts from `dev/bin` folder are storing runtime information in `dev/bin/_deploy/local_env` file, in order to keep a state of the current run.

### Deploy dev environment

This will deploy/start the different services:

* on cluster
  * keycloak
  * prometheus operator CRDs
* with docker-compose
  * manager db (postgres)
  * localstack
  * prometheus
  * grafana
* locally
  * manager
  * shard operator

To deploy and run all dev resources, just run:

```bash
./dev/bin/deploy.sh -s -l -k
```

This will:

* start the configured cluster (`-s` option)
* build the local image and push them to cluster (`-l` option)
* run manager and shard operator services locally
  * logs from those services can be found in `dev/bin/logs` folder

The manager URL is http://localhost:8080.

### Deploy "demo" environment on cluster

This will deploy all resources on the cluster:

* keycloak
* prometheus operator CRDs
* manager db (postgres)
* localstack
* prometheus
* grafana
* manager
* shard operator

To deploy and run all dev resources, just run:

```bash
./dev/bin/deploy.sh -s -l -k -a
```

This will:

* start the configured cluster (`-s` option)
* build the local image and push them to cluster (`-l` option)
* setup kafka resources on managed services (`-k` option)
* deploy all services on cluster (`-a` option)
  
The manager URL is http://localhost:80/manager.

## Undeploy resources

To undeploy all resources, just run

```bash
./dev/bin/undeploy.sh
```

If you also want to stop the cluster, just add the `-s` option.

## Detailed scripts from the `dev/bin` folder

The `deploy` and `undeploy` scripts are using other scripts from `dev/bin` folder.

You will find below some explanations of those.

### Setup Managed Kafka

A remote Managed Kafka instance is required for the internal communication between components of the system.

The development team is asked to use the *shared kafka instance* we have deployed under our organization. 

**Follow the instructions [here](https://docs.google.com/document/d/1fMnHUmGnO-GZuY2BuEe02_prJs3_7QZdSZ5-SfOJ_Yk) to setup the service accounts (you need to be part of the `rhose` google group to access the document)**. Those service accounts will be used by the local services to create/delete topics and acls on the shared kafka cluster.

In case you want to use different credentials, you can replace the JSON files in the [credentials](bin/credentials) folder:

| Name                         | Content                                         |
|:-----------------------------|-------------------------------------------------|
| `<instance_name>.json`       | Kafka cluster data (e.g. bootstrap host)        |
| `<instance_name>-admin.json` | Admin service account credentials               |
| `<instance_name>-ops.json`   | Operational service account credentials         |
| `<instance_name>-mc.json`    | Managed Connectors service account credentials  |

### Cluster start

A local Minikube / Kind cluster is needed to deploy parts of the system.

The [cluster-start.sh](bin/cluster-start.sh) script takes care of configuring it for you.
It is **idempotent**, so it can be run whenever you want to make sure the Minikube cluster is configured properly.

Just run it without arguments:

```bash
./dev/bin/cluster-start.sh
```

Depending on the cluster configured in the `localconfig` file, it will call the `dev/bin/cluster-minikube-start.sh` or `dev/bin/cluster-kind-start.sh`.  
`minikube` is the default if nothing is configured.

Check the script header for the supported env variables that can be used to configure the Minikube / Kind cluster (e.g CPUs, memory, ...).

### Cluster stop

Like the cluster start, you can use the [cluster-stop.sh](bin/cluster-stop.sh) file to stop the current running cluster.

```bash
./dev/bin/cluster-stop.sh
```

This script will call the `dev/bin/cluster-minikube-stop.sh` or `dev/bin/cluster-kind-stop.sh` script, depending on your current setup.

### Build and deploy container images for the cluster

The [cluster-load-containers.sh](bin/cluster-load-containers.sh) script takes care of building the Docker images for the different system components and load them to Minikube/Kind internal registry.  
Run it whenever the code changes and they need to be updated.

Just run it without arguments:

```bash
./dev/bin/cluster-load-containers.sh
```

### Load resources to the cluster

4 scripts helps you to load some resources to the cluster:

* [cluster-prepare-resources.sh](bin/cluster-prepare-resources.sh)  
  should be called before any call to one of the 3 others. This will initialize the `_deploy/kustomize` script for local deployment to the cluster.
* [cluster-deploy-keycloak](bin/cluster-deploy-keycloak.sh)  
  sets up the Keycloak service in the cluster
* [cluster-deploy-prometheus-crd.sh](bin/cluster-deploy-prometheus-crd.sh)  
  uploads the Prometheus crds so that servicemonitor can be created on the cluster
* [cluster-deploy-all-cluster-resources](bin/cluster-deploy-all-cluster-resources.sh)  
  deploys all resources in the cluster

### Start/Stop local manager and shard operator

* [local-manager-start.sh](bin/local-manager-start.sh)  
  Runs the manager locally
* [local-shard-start.sh](bin/local-shard-start.sh)  
  Runs the shard operator locally
* [local-quarkus-service-stop.sh](bin/local-quarkus-service-stop.sh)  
  Stops a locally running service. It takes the project dir as first argument from where the `mvn quarkus:dev` was launched.

## Needed folders

When you run those `dev/bin` scripts, it will store information in different folders to keep a state of the current.

There are 2 added folders in the `dev` which are useful to those scripts:

* _deploy
* credentials

### _deploy folder

This folder is used to store temporary config / env for the current run.

* local_env  
  contains the current env
* kustomize  
  is a copy of the `kustomize` folder where some local changes are applied to deploy the resources.

## Setup dev environment by steps

An alternative to the `dev/bin/deploy.sh` script is to run the different scripts manually.

You will find below the following commands.

### Start cluster and deploy dev resources on it

```bash
./dev/bin/cluster-prepare-resources.sh
./dev/bin/kafka-setup.sh
./dev/bin/cluster-start.sh
./dev/bin/cluster-load-containers.sh
./dev/bin/cluster-deploy-keycloak.sh
./dev/bin/cluster-deploy-prometheus-crd.sh
```

### Start supporting resources for Fleet Manager

We provide a `docker-compose.yaml` file that you can use to spin up all the resources that the Fleet Manager needs to run (postgres, prometheus and grafana).

```bash
docker-compose -f dev/docker-compose/docker-compose.yml up -d
```

The above command will start the containers in background. To check the status of the containers, run:

```bash
docker-compose -f dev/docker-compose/docker-compose.yml ps
```

and to check the logs, run:

```bash
docker-compose -f dev/docker-compose/docker-compose.yml logs
```

### Start the Fleet Manager

The [local-manager-start.sh](bin/local-manager-start.sh) script starts the Fleet Manager with all the configurations.

Just run it without arguments:

```bash
./dev/bin/local-manager-start.sh
```

Whatever argument you'll pass to it, it will be forwarded to Maven and can be used for temporary configurations.

### Start the Fleet Shard

The [local-shard-start.sh](bin/local-shard-start.sh) script starts the Fleet Shard with all the configurations.

Just run it without arguments:

```bash
./dev/bin/local-shard-start.sh
```

Whatever argument you'll pass to it, it will be forwarded to Maven and can be used for temporary configurations.

## Send Requests

Now you're ready to test. Follow the instructions in our [demo](../DEMO.md) to send requests to your locally running infrastructure.

## Generate traffic automatically

**IMPORTANT: Be careful! If you run this on against your local environment, the infrastructure is going to consume a lot of resources.**

If you want to generate some traffic automatically, we provide the script `generate_traffic.py` that you can run with 

```bash
python3 dev/utils/generate_traffic.py --manager=http://localhost:8080 --keycloak=http://localhost:8180 --username=kermit --password=thefrog --bad_request_rate=0.2 --match_filter_rate=0.2
```

The script runs forever, on linux machines press `CTRL+C` to stop it (or just send a SIGINT signal to the process according to the operating system you are using).

With the parameters `--manager`, `--keycloak`, `--username` and `--password` you can configure the script so to target any environment (for example the demo environment).

The grafana dashboards in the `grafana` folder are just for development purposes, but it's good to keep them in sync with the dashboards deployed in the demo environment (those dashboards are located at `kustomize/overlays/prod/observability/grafana`).

## Binaries migration guide (Apr 22)

## Renamed scripts

Some scripts from the `bin` folder have been renamed for more coherence.  
Here is the list of new names:

| Old name                           | New name                     |
|:-----------------------------------|------------------------------|
| `shard-run.sh`                     | `local-shard-start.sh`       |
| `manager-run.sh`                   | `local-manager-start.sh`     |
| `minikube-start.sh`                | `cluster-start.sh`           |
| `minikube-build-docker-images.sh`  | `cluster-load-containers.sh` |

## Config files moved

On the first execution of the `bin/common.sh` script, configuration files will be automatically transferred to the `config` folder.  
Here are the moves done:

* bin/localconfig -> config/localconfig
* bin/interaction/environment -> config/interactionconfig

