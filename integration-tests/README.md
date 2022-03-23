# integration-tests

Module contains integration tests verifying end to end functionality in real cluster.  
The tests are executed using command:

```bash
mvn clean verify -Pcucumber -Devent-bridge.manager.url=<MANAGER_URL> -Dkeycloak.realm.url=<KEYCLOAK_URL>
```

Optionally the `keycloak.realm.url` parameter can be omitted by setting `OB_TOKEN` environment variable.  
Just a note, the `OB_TOKEN` value has to be the token value itself, without the `Bearer` prefix.  

## Prerequisites

- Cluster is up and running
- All components are deployed  
  *using kustomize overlays or via `dev/bin/*-run.sh` scripts for local development*
- Configure `event-bridge.manager.url` and `keycloak.realm.url`  
  *in pom.xml, via the maven command mentioned above or use local-tests script (see below)*

## Local testing

Alternatively, if you started the environment for local development via `dev/bin/manager-run.sh` or `kustomize/startMinikubeDeployLocalDev.sh` script, you can use the `integration-tests/run-local-tests.sh` script to launch the test with all local parameters set automatically.

Script has 2 options:

- `-t TAGS`  
  Specify which tags to execute  
  See also https://github.com/cucumber/cucumber-jvm/tree/main/junit-platform-engine#tags
- `-p`  
  Run tests in parallel

## Keycloak token authentication

By Default test runs with those authentication parameters for Keycloak:

```xml
<bridge.token.username>kermit</bridge.token.username>
<bridge.token.password>thefrog</bridge.token.password>
<bridge.client.id>event-bridge</bridge.client.id>
<bridge.client.secret>secret</bridge.client.secret>
```

You can update the parameters needs to configure in `integration-tests/pom.xml` or via the maven command.

## Feature file placeholders

Curently there is a possibility to use these placeholders in feature files:

- ${env.`<System property name>`} to use System property
- ${bridge.`<Bridge name>`.cloud-event.`<Cloud event id>`.id} to use "System cloud event id" (Cloud event id which is actually used for Cloud event invocation)
