# sandbox

trigger CI

[![Bridge - CD](https://github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/actions/workflows/CD.yml/badge.svg)](https://github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/actions/workflows/CD.yml)

This repository stores the code for the bridge project.

## Running the all-in-one application locally

In the current status, the Manager, the Shard, the Ingress and the Executors are packaged in one single application that we call `all-in-one`.

Pre-requisites
Local development is supported on Unix or Mac environments. Windows is not supported.

You will need the following installed:

- Docker
- Docker-Compose
- Maven

The latest versions of these dependencies are fine.

Start `docker-compose` by running from the root directory of this repository

```bash
docker-compose -f dev/docker-compose.yml up
```

and then 

```bash
mvn clean install -DskipTests && mvn clean compile quarkus:dev -f runner/pom.xml
```

You can then access the Swagger-ui at `http://localhost:8080/q/swagger-ui`

# DEMO 

A demonstration of the service is provided [here](DEMO.md).

# How to deploy the platform service to a Kubernetes cluster

See the specific documentation [here](kustomize/README.md)
