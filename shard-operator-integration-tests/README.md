# Shard Operator Integration Tests

Module contains integration tests verifying Shard operator functionality in real cluster. The tests are executed using command `mvn clean verify -Pcucumber`.

Prerequisites for running the tests:

1. Cluster is up and running
2. Ingress is installed in the cluster
3. Shard operator is installed in cluster
4. Local kubeconfig in initialized and connected to the cluster (i.e. local execution of `kubectl cluster-info` is able to reach the cluster and return cluster info)

Tests are implemented using [Cucumber-JVM](https://github.com/cucumber/cucumber-jvm), test scenarios are written using [Gherkin syntax](https://cucumber.io/docs/gherkin/reference/).

Every test scenario runs in a dedicated namespace which is deleted after the test execution. If user terminates test execution prematurely (CTRL + C) then namespace is not deleted automatically, needs to be deleted by user.

Logs are stored in `target/logs` directory, contains logs of all containers and events in a namespace used to run the tests.

## Supported Maven parameters

- `tags`: Used to specify a single scenario to be executed. Set the parameter to (for example) `@wip` and annotate a scenario which you want to execute by same tag (`@wip`).
- `parallel`: User to allow running tests in parallel.

## Local testing

Alternatively, if you started the environment for local development via `dev/bin/manager-run.sh` or `kustomize/startMinikubeDeployLocalDev.sh` script, you can use the `shard-operator-integration-tests/run-local-tests.sh` script to launch the test with all local parameters set automatically.

Script has 2 options:

- `-t TAGS`  
  Specify which tags to execute  
  See also https://github.com/cucumber/cucumber-jvm/tree/main/junit-platform-engine#tags
- `-p`  
  Run tests in parallel