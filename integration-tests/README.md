# E2E Integration tests

Module contains integration tests verifying end to end functionality in real cluster.

## Test design

Tests are written using [Gherkin syntax](https://cucumber.io/docs/gherkin/reference/).
Test steps are implemented in Java using [Cucumber JVM framework](https://github.com/cucumber/cucumber-jvm).

### Test scenarios

Main test scenarios are stored in [features folder](src/test/resources/features).

Scenarios are splitted into dedicated feature files:
- [bridge.feature](src/test/resources/features/bridge.feature) - Contains CRUD tests for Bridge
- [ingress.feature](src/test/resources/features/ingress.feature) - Contains basic tests for exposed Bridge endpoint
- [processor.feature](src/test/resources/features/processor.feature) - Contains CRUD tests for Processor
- [processor-template.feature](src/test/resources/features/processor-template.feature) - Contains test coverage for Processor transformation template
- [action-slack.feature](src/test/resources/features/action-slack.feature) (and other `action-xxx`)- Contains tests for specific Processor action

In separate folder there is placed test scenario for running periodic check against existing Processor instance - [action-slack.feature](src/test/resources/periodical-slack-check-features/action-slack.feature)

### Test steps implementation

Main tests are executed as [JUnit 5 test suite](src/test/java/com/redhat/service/smartevents/integration/tests/RunCucumberTest.java).

Test steps are stored in [steps folder](src/test/java/com/redhat/service/smartevents/integration/tests/steps). This folder contains also [hooks](src/test/java/com/redhat/service/smartevents/integration/tests/steps/Hooks.java) taking care of environment initialization and cleanup.

Tests steps leverage Java REST client classes for various services stored in [resources package](src/test/java/com/redhat/service/smartevents/integration/tests/resources).

Informations are passed between steps using injected context, it is implementation is in [context folder](src/test/java/com/redhat/service/smartevents/integration/tests/context). This package also contains a resolver interpreting placeholders in scenarios, replacing them with expected values.

### Test identifiers vs system identifiers

In the tests we put a strong emphasis for test independence and ability to execute tests in parallel. Therefore all identifiers used to create resources (Bridges, Processors) and send cloud events have to be unique.
On the other side sometimes in the test we need to reference this resources ids, i.e. to check its value in external systems.

To combine both requirements we introduced a concept of test identifier and system identifier.
Test identifier is a specific resource identifier used in a scenario (for example Bridge name). When a Bridge is created the test engine takes the test identifier, generates a unique system identifier corresponding to the test identifier and store the test to system identifier mapping in a context. System identifier is then used as a real value when interacting with remote system.

### Feature file placeholders

It is possible to use various placeholders in feature files, allowing dynamic acess to runtime values and system identifiers. The placeholders use format `${placeholder-value}`.

Currently, there is a possibility to use these placeholders in feature files:
- ${env.`<System property name>`} to use a System property
- ${bridge.`<Bridge name>`.id} to use actual "Bridge id"
- ${cloud-event.`<Cloud event id>`.id} to use "System cloud event id" (Cloud event id which is actually used for Cloud event invocation)
- ${uuid.`<Uuid name>`} TODO

## Test execution

### Local testing

If you deployed application locally (either by using [startMinikubeDeployLocalDev.sh script](../kustomize/startMinikubeDeployLocalDev.sh) or [running manager and operator locally](../dev/README.md)) you can use [run-local-tests.sh](run-local-tests.sh) script to launch the tests with all local parameters set automatically.

Script has 3 options:
- `-t TAGS`  
  Specify which tags to execute  
  See also https://github.com/cucumber/cucumber-jvm/tree/main/junit-platform-engine#tags
- `-p`  
  Run tests in parallel
- `-k`  
  Keep created data, aka do not perform cleanup. Mostly used for local test run and debug.


### Remote testing

For remote testing you can execute tests using Maven command:

```bash
export OPENSHIFT_OFFLINE_TOKEN=<openshift-offline-token> # To obtain it, go to https://console.redhat.com/openshift/token
mvn clean verify -Pcucumber -Devent-bridge.manager.url=<MANAGER_URL> -Dkeycloak.realm.url=<KEYCLOAK_URL> -Dbridge.client.id=<CLIENT_ID>
# Example:
# <CLIENT_ID>="cloud-services"
# <KEYCLOAK_URL>="https://sso.redhat.com/auth/realms/redhat-external"
```

### Remote system access configuration

To properly run the tests locally you need to specify environment variables to access remote systems - Slack and webhook.site:
- `SLACK_WEBHOOK_URL` - Slack WebHook URL
- `SLACK_WEBHOOK_TOKEN` - Access token for Slack WebHook
- `SLACK_CHANNEL` - Slack channel id (different from Slack channel name)
- `WEBHOOK_SITE_UUID` - UUID to be used for tests interacting with webhook.site


### Keycloak token authentication

By Default test runs with those authentication parameters for Keycloak:

```xml
<bridge.client.id>kermit</bridge.client.id>
<bridge.client.secret>N5TW1EfuIcQsplRsLXJ1aE3DZZMPN5ZH</bridge.client.secret>
```

You can update the parameters needs to configure in `integration-tests/pom.xml` or via the maven command.
