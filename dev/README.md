# DEV 

## Requirements

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

Deploy the kafka resources with 

```shell
kustomize build kustomize/overlays/minikube/kafka | kubectl apply -f -
```

And keycloak

```shell
kustomize build kustomize/overlays/minikube/keycloak | kubectl apply -f -
```

Wait until all the resources have been deployed (it might take a while for a brand new cluster).

```bash
kubectl wait pod -l app.kubernetes.io/instance=my-cluster --for=condition=Ready --timeout=600s -n kafka
```

Deploy the ServiceMonitor CRD from the Prometheus operator with 

```bash
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/kube-prometheus/v0.9.0/manifests/setup/prometheus-operator-0servicemonitorCustomResourceDefinition.yaml
```

## Managed Kafka integration (optional)

Optionally, the manager can be configured to integrate with Managed Kafka.

First of all, you need to [install the rhoas CLI](https://access.redhat.com/documentation/en-us/red_hat_openshift_streams_for_apache_kafka/1/guide/f520e427-cad2-40ce-823d-96234ccbc047)
and login using a Managed Kafka test account (ask the dev team for credentials) and its **offline token**. From now on a successful login is assumed.

Change the Kafka instance (`my-test-instance`) and service account (`my-test-instance-admin`) names according to your needs.

### Create a test Kafka instance

Create a Kafka test instance. These instances are automatically deleted after 48h and you will need to repeat the process.

```bash
rhoas kafka create --name my-test-instance
```

Monitor the creation status with `rhoas status` and wait for it to be `ready`.

### Create the admin service account

```bash
rhoas service-account create --output-file=my-test-instance-admin.json --file-format=json --overwrite --short-description=my-test-instance-admin
```

**Note:** this doesn't get deleted, so no need to repeat this command if your Kafka instance is gone.

### Set permissions (ACLs) to the admin service account

```bash
service_account_id=$( jq -r '.clientID' 'my-test-instance-admin.json' )

rhoas -v kafka acl grant-admin -y --service-account "${service_account_id}"
rhoas kafka acl create -y --user "${service_account_id}" --permission allow --operation create --topic all
rhoas kafka acl create -y --user "${service_account_id}" --permission allow --operation delete --topic all
```

### Create the operational service account

```bash
rhoas service-account create --output-file=my-test-instance-ops.json --file-format=json --overwrite --short-description=my-test-instance-ops
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

**If you configured Managed Kafka integration as described above, export the following env variables:**

```bash
export EVENT_BRIDGE_FEATURE_FLAGS_RHOAS_ENABLED=true
export EVENT_BRIDGE_RHOAS_MGMT_API_HOST=https://api.openshift.com
export EVENT_BRIDGE_RHOAS_INSTANCE_API_HOST=http://admin-server-<kafka_instance_bootstrap_host>
export EVENT_BRIDGE_RHOAS_SSO_RED_HAT_AUTH_SERVER_URL=https://sso.redhat.com/auth/realms/redhat-external
export EVENT_BRIDGE_RHOAS_SSO_RED_HAT_CLIENT_ID=cloud-services
export EVENT_BRIDGE_RHOAS_SSO_RED_HAT_REFRESH_TOKEN=<test_account_offline_token>
export EVENT_BRIDGE_RHOAS_SSO_MAS_AUTH_SERVER_URL=https://identity.api.openshift.com/auth/realms/rhoas
export EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_ID=$( jq -r '.clientID' 'my-test-instance-admin.json' )
export EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_SECRET=$( jq -r '.clientSecret' 'my-test-instance-admin.json' )
export RHOAS_OPS_ACCOUNT_CLIENT_ID=$( jq -r '.clientID' 'my-test-instance-ops.json' )
```

**From the root of the project** run the Fleet Manager application with 

```bash
chmod +x ./dev/run_manager.sh
./dev/run_manager.sh
```

### Start the Fleet Shard Operator

**Open another terminal.**

**From the root of the project** run the Fleet Shard Operator with 

```bash 
chmod +x ./dev/run_shard.sh
./dev/run_shard.sh
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
