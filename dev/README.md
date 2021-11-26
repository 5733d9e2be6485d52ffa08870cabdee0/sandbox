# DEV 

## First time setup

This section contains all the instructions to setup your environment in order to deploy the infrastructure locally. 

First of all, you need to install minikube following [this](https://minikube.sigs.k8s.io/docs/start/) guide. (Tested with minikube v1.16.0 on Redhat 8.4)

Once you have installed minikube, start a new cluster with 

```bash 
minikube --memory 8192 --cpus 4 start  --kubernetes-version=v1.20.0
```

You can change the memory and cpu settings according to your system. **Other versions of kubernetes have not been tested**.

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

## Development environment

Start your minikube cluster with 

```bash
minikube start
```

When you want deploy the entire platform, you have to point to the internal minikube registry with the following command

``bash
eval $(minikube -p minikube docker-env)
``

So that all the docker images that you build locally will be pushed to the internal registry of minikube. 

**From the root of the project**, build all the modules with 

```bash 
mvn clean install -DskipTests -Dquarkus.container-image.build=true
```

We provide a `docker-compose.yaml` file that you can use to spin up all the resources that the manager needs to run (keycloak, postgres, prometheus and grafana). 

**From another terminal** (otherwise the images will be pulled into your minikube internal registry) and the root of the project, run

```bash
docker-compose -f dev/docker-compose/docker-compose.yml up
```

**From the root of the project** run the manager application with 

```bash
mvn clean compile -f manager/pom.xml quarkus:dev
```

Run the shard operator from the root of the project with 

```bash 
mvn clean compile -f shard-operator/pom.xml -Dquarkus.http.port=1337 -Pminikube quarkus:dev
```

## Generate traffic automatically

NOTE: Be careful! If you run this on against your local environment, the infrastructure is going to consume a lot of resources.

If you want to generate some traffic automatically, we provide the script `generate_traffic.py` that you can run with 

```bash
python3 generate_traffic.py --manager=http://localhost:8080 --keycloak=http://localhost:8180 --username=kermit --password=thefrog --bad_request_rate=0.2 --match_filter_rate=0.2
```

The script runs forever, on linux machines press `CTRL+C` to stop it (or just send a SIGINT signal to the process according to the operating system you are using).

With the parameters `--manager`, `--keycloak`, `--username` and `--password` you can configure the script so to target any environment (for example the demo environment).

The grafana dashboards in the `grafana` folder are just for development purposes, but it's good to keep them in sync with the dashboards deployed in the demo environment (those dashboards are located at `kustomize/overlays/prod/observability/grafana`).