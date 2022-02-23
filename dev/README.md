# DEV

## Requirements

**IMPORTANT: unless specified, all the commands are supposed to be executed from the root of the repository**

You will need the following installed locally on your machine to support local development:

* [Minikube v1.16.0](https://minikube.sigs.k8s.io/docs/start/)
* [Docker Engine](https://docker.com)
  * The most recent version should be fine.
* [Docker Compose v1.29.2](https://github.com/docker/compose)
* [kustomize](https://kustomize.io/)
* [Maven v3.8.1](https://maven.apache.org/)
* [Java 11](https://adoptopenjdk.net/)
* [jq](https://stedolan.github.io/jq/)
* [curl](https://curl.se/) (or any other HTTP client)
* [rhoas CLI](https://access.redhat.com/documentation/en-us/red_hat_openshift_streams_for_apache_kafka/1/guide/fa4bad02-10f2-4ef3-be34-7edc1337e7ee)
* Many of us use and recommend [PostMan](https://postman.com) for testing our API instead of curl.

### macOS users:

Do not install Minikube via `brew`. 
Download the specific v.1.16.0 from the [Minikube release page](https://github.com/kubernetes/minikube/releases/tag/v1.16.0).

## Preliminary configuration

_A Red Hat account is required to log in to the remote services (e.g. Managed Kafka and Managed Connectors)._

This guide uses [a set of BASH scripts](bin) to make the local development experience easier.

They can be configured using environment variables. Some of them are required, other optionals.
Every script contains a header explaining which variables it reads.

Here is a list of the **required** environment variable:

| Name                            | Description                                                                                                                         |
|:--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `MANAGED_CONNECTORS_CLUSTER_ID` | ID of the cluster where Managed Connectors are deployed, required to use Managed Connectors based actions. Skip it if you need them. |
| `MANAGED_KAFKA_INSTANCE_NAME`   | Name of the remote Managed Kafka instance, used to configure the instance itself and the related service accounts.                  |
| `OPENSHIFT_OFFLINE_TOKEN`       | OpenShift offline token. To obtain it, go to https://console.redhat.com/openshift/token                                             |

### Configuration via "localconfig" file

If a file named `localconfig` is created in the [dev/bin](bin) folder, all its content is loaded before every script.

This allows to define fixed local configurations once for all.

Check the [localconfig-example](bin/localconfig-example) file for an example of how to use it.

## Managed Kafka instance setup

A remote Managed Kafka instance is required for the internal communication between components of the system.

The [kafka-setup.sh](bin/kafka-setup.sh) script takes care of configuring it for you.
It is **idempotent**, so it can be run whenever you want to make sure the Kafka cluster is configured properly.

Just run it without arguments:

```bash
./dev/bin/kafka-setup.sh
```

**IMPORTANT #1:** the script will perform `rhoas login` for you. Follow the instructions and **log in with your Red Hat account**.

**IMPORTANT #2:** Managed Kafka test instances expire after 48 hours, so you will need to rerun this script at least every 48 hours.

### Credentials folder

The script creates some JSON files inside the [credentials](bin/credentials) folder:

| Name                         | Content                                         |
|:-----------------------------|-------------------------------------------------|
| `<instance_name>.json`       | Kafka cluster data (e.g. bootstrap host)        |
| `<instance_name>-admin.json` | Admin service account credentials               |
| `<instance_name>-ops.json`   | Operational service account credentials         |
| `<instance_name>-mc.json`    | Managed Connectors service account credentials  |

**IMPORTANT: if you believe the credentials of one of the service accounts are somehow outdated/wrong, simply delete the corresponding file and re-run the script:
it will create new working credentials for that service account.**

## Minikube cluster setup

A local Minikube cluster is needed to deploy parts of the system

The [minikube-start.sh](bin/minikube-start.sh) script takes care of configuring it for you.
It is **idempotent**, so it can be run whenever you want to make sure the Minikube cluster is configured properly.

Just run it without arguments:

```bash
./dev/bin/minikube-start.sh
```

Check the script header for the supported env variables that can be used to configure the Minikube cluster (e.g CPUs, memory, ...).

### macOS users:

It's important to set the `hyperkit` driver before starting the Minikube cluster due to [this bug](https://github.com/kubernetes/minikube/issues/7332)

Either export this env variable or add this to `localconfig` file (the latter method is suggested):

```bash 
MINIKUBE_DRIVER=hyperkit
```

### Fedora users

With Fedora 34, there is an issue in spotting the coredns pod due to a cgroup issue with docker/podman.

One workaround is to use the `cri-o` container runtime with `podman` driver.

Either export this env variable or add this to `localconfig` file (the latter method is suggested):

```bash 
MINIKUBE_DRIVER=podman
MINIKUBE_CONTAINER_RUNTIME=cri-o
```

## Build container images for Minikube

The [minikube-build-docker-images.sh](bin/minikube-build-docker-images.sh) script takes care of building the Docker images
for the different system components and store them in Minikube internal registry.
Run it whenever the code changes and they need to be updated.

Just run it without arguments:

```bash
./dev/bin/minikube-build-docker-images.sh
```

## Start supporting resources for Fleet Manager

We provide a `docker-compose.yaml` file that you can use to spin up all the resources that the Fleet Manager needs to run (keycloak, postgres, prometheus and grafana). 

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

## Start the Fleet Manager

The [manager-run.sh](bin/manager-run.sh) script starts the Fleet Manager with all the configurations.

Just run it without arguments:

```bash
./dev/bin/manager-run.sh
```

Whatever argument you'll pass to it, it will be forwarded to Maven and can be used for temporary configurations.

## Start the Fleet Shard

The [shard-run.sh](bin/shard-run.sh) script starts the Fleet Shard with all the configurations.

Just run it without arguments:

```bash
./dev/bin/shard-run.sh
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

## Knative

The [knative-installer.sh](bin/knative-installer.sh) script applies Knative Eventing and Knative Eventing Kafka components on the Kubernetes cluster.

Just run it without arguments:

```bash
./dev/bin/knative-installer.sh
```
