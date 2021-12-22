# DEV 

## Requirements

You will need the following installed locally on your machine to support local development:

* [Minikube v1.16.0](https://minikube.sigs.k8s.io/docs/start/)
* [Docker Engine](https://docker.com)
  * The most recent version should be fine.
* [Docker Compose v1.29.2](https://github.com/docker/compose)
* [Maven v3.8.1](https://maven.apache.org/)
* [Java 11](https://adoptopenjdk.net/)
* [jq](https://stedolan.github.io/jq/)
* [curl](https://curl.se/) (or any other HTTP client)
    * Many of us use and recommend [PostMan](https://postman.com) for testing our API instead of curl.

### macOS users:

Do not install Minikube via `brew`. 
Download the specific v.1.16.0 from the [Minikube release page](https://github.com/kubernetes/minikube/releases/tag/v1.16.0).

## First time setup

This section contains all the instructions to setup your environment in order to deploy the infrastructure locally. 

Configure a new Minikube Cluster with the following: 

```bash 
minikube --memory=8192 --cpus=4 --kubernetes-version=v1.20.0 start  
```

### macOS users:

It's important to set the `hyperkit` driver before starting the Minikube cluster due to [this bug](https://github.com/kubernetes/minikube/issues/7332)

```bash 
minikube --driver=hyperkit --memory=8192 --cpus=4 --kubernetes-version=v1.20.0 start  
```

You can change the memory and cpu settings according to your system. **Other versions of Kubernetes have not been tested**.

Enable the `ingress` and `ingress-dns` addons with the following commands: 

```bash
minikube addons enable ingress
minikube addons enable ingress-dns
```

Create a namespace for `kafka`

```bash
kubectl create ns kafka
```

From the root of the project, deploy the kafka infrastructure with 

```bash
kubectl apply -f dev/kubernetes/kafka/00_strimzi.yaml -n kafka
kubectl apply -f dev/kubernetes/kafka/01_kafka.yaml -n kafka
kubectl apply -f dev/kubernetes/kafka/02_kafka-topics.yaml -n kafka
```

Wait until all the resources have been deployed (it might take a while for a brand new cluster).

```bash
kubectl wait pod -l app.kubernetes.io/instance=my-cluster --for=condition=Ready --timeout=600s -n kafka
```

Deploy the ServiceMonitor CRD from the Prometheus operator with 

```bash
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/kube-prometheus/v0.9.0/manifests/setup/prometheus-operator-0servicemonitorCustomResourceDefinition.yaml
```

## Development environment

If not already running, start your Minikube cluster with 

```bash
minikube start
```

### Build All Container Images

When you want deploy the entire platform, you have to point to the internal minikube registry with the following command

``bash
eval $(minikube -p minikube docker-env)
``

So that all the docker images that you build locally will be pushed to the internal registry of minikube. 

**From the root of the project**, build all the modules with 

```bash 
mvn clean install -DskipTests -Dquarkus.container-image.build=true
```

### Start All Supporting Resources

We provide a `docker-compose.yaml` file that you can use to spin up all the resources that the manager needs to run (keycloak, postgres, prometheus and grafana). 

**From another terminal** (otherwise the images will be pulled into your minikube internal registry) and the root of the project, run

```bash
docker-compose -f dev/docker-compose/docker-compose.yml up
```

The above command will not exit. Instead it will print the boot logs for all supporting services to STDOUT. Ensure that
the boot sequence for all supporting infrastructure looks clean with no obvious errors.

From another terminal, you can check the status of all containers using `docker ps -a`, which should give you output similar
to the following:

```bash
> docker ps -a
CONTAINER ID   IMAGE                    COMMAND                  CREATED          STATUS          PORTS                                        NAMES
8e11a5fc333e   grafana/grafana:6.6.1    "/run.sh"                38 minutes ago   Up 38 minutes                                                docker-compose_grafana_1
bfaea280bff3   prom/prometheus:v2.8.0   "/bin/prometheus --c…"   38 minutes ago   Up 38 minutes                                                docker-compose_prometheus_1
24af8a014c54   jboss/keycloak:10.0.1    "/opt/jboss/tools/do…"   38 minutes ago   Up 38 minutes   8080/tcp, 8443/tcp, 0.0.0.0:8180->8180/tcp   event-bridge-keycloak
1d14ea702f92   postgres:13.1            "docker-entrypoint.s…"   38 minutes ago   Up 38 minutes   0.0.0.0:5432->5432/tcp                       event-bridge-postgres
```

### Start the Fleet Manager

**Open another terminal.**

**From the root of the project** run the Fleet Manager application with 

```bash
mvn clean compile -f manager/pom.xml quarkus:dev
```

### Start the Fleet Shard Operator

**Open another terminal.**

**From the root of the project** run the Fleet Shard Operator with 

```bash 
mvn clean compile -f shard-operator/pom.xml -Dquarkus.http.port=1337 -Pminikube quarkus:dev
```

### Send Requests

Follow the instructions in our [demo](../DEMO.md) to send requests to your locally running infrastructure.

## Generate traffic automatically

NOTE: Be careful! If you run this on against your local environment, the infrastructure is going to consume a lot of resources.

If you want to generate some traffic automatically, we provide the script `generate_traffic.py` that you can run with 

```bash
python3 generate_traffic.py --manager=http://localhost:8080 --keycloak=http://localhost:8180 --username=kermit --password=thefrog --bad_request_rate=0.2 --match_filter_rate=0.2
```

The script runs forever, on linux machines press `CTRL+C` to stop it (or just send a SIGINT signal to the process according to the operating system you are using).

With the parameters `--manager`, `--keycloak`, `--username` and `--password` you can configure the script so to target any environment (for example the demo environment).

The grafana dashboards in the `grafana` folder are just for development purposes, but it's good to keep them in sync with the dashboards deployed in the demo environment (those dashboards are located at `kustomize/overlays/prod/observability/grafana`).
