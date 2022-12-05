# E2E Integration tests for API v2

Module contains integration tests verifying end to end functionality in real cluster.

## Test design

Tests are written using [Gherkin syntax](https://cucumber.io/docs/gherkin/reference/).
Test steps are implemented in Java using [Cucumber JVM framework](https://github.com/cucumber/cucumber-jvm).
For connection to external systems the test suite leverages [TNB - System X](https://github.com/tnb-software/TNB/tree/main/system-x).

### Test scenarios

Main test scenarios are stored in [features folder](src/test/resources/features).

Scenarios are split into dedicated feature files:
- [bridge.feature](src/test/resources/features/bridge.feature) - Contains CRUD tests for Bridge
- [ingress.feature](src/test/resources/features/ingress.feature) - Contains basic tests for exposed Bridge endpoint
- [processor.feature](src/test/resources/features/processor.feature) - Contains CRUD tests for Processor
- [processor-template.feature](src/test/resources/features/processor-template.feature) - Contains test coverage for Processor transformation template
- [action-slack.feature](src/test/resources/features/action-slack.feature) (and other `action-xxx`)- Contains tests for specific Processor action

Rest of scenarios are kept in two different feature folders:

- [performance-features](src/test/resources/performance-features) - Contains different performance test scenarios
- [periodical-slack-check-features](src/test/resources/periodical-slack-check-features) - Contains test scenarios for running periodic check against existing Processor instance - [action-slack.feature](src/test/resources/periodical-slack-check-features/action-slack.feature)

### Test steps implementation

Main tests are executed as [JUnit 5 test suite](src/test/java/com/redhat/service/smartevents/integration/tests/v2/RunCucumberTest.java).

Test steps are stored in [steps folder](src/test/java/com/redhat/service/smartevents/integration/tests/v2/steps). This folder contains also [hooks](src/test/java/com/redhat/service/smartevents/integration/tests/v2/steps/Hooks.java) taking care of environment initialization and cleanup.

Test steps leverage Java client classes for various services stored in [resources package](src/test/java/com/redhat/service/smartevents/integration/tests/v2/resources).

Information is passed between steps using injected context, it is implementation is in the [common context folder](../../integration-tests-common/src/main/java/com/redhat/service/smartevents/integration/tests/context) shared by tests for different API versions. This package also contains a resolver interpreting placeholders in scenarios, replacing them with expected values.

### Test identifiers vs system identifiers

In the tests we put a strong emphasis for test independence and ability to execute tests in parallel. Therefore, all identifiers used to create resources (Bridges, Processors) and send cloud events have to be unique.
On the other side sometimes in the test we need to reference these resources ids, i.e. to check its value in external systems.

To combine both requirements we introduced a concept of test identifier and system identifier.
Test identifier is a specific resource identifier used in a scenario (for example Bridge name). When a Bridge is created the test engine takes the test identifier, generates a unique system identifier corresponding to the test identifier and store the test to system identifier mapping in a context. System identifier is then used as a real value when interacting with remote system.

### Feature file placeholders

It is possible to use various placeholders in feature files, allowing dynamic access to runtime values and system identifiers. The placeholders use format `${placeholder-value}`.

Currently, there is a possibility to use these placeholders in feature files:
- ${aws.access-key} to use an AWS access key belonging to a particular AWS account to connect to
- ${aws.region} to use a particular AWS region belonging to an AWS account to connect to
- ${aws.secret-key} to use an AWS secret key belonging to a particular AWS account to connect to
- ${aws.sqs.`<Queue name>`} to use an AWS SQS queue
- ${bridge.`<Bridge name>`.endpoint.base} to use Bridge endpoint base URL (i.e. for http://localhost:80/something it will return http://localhost:80)
- ${bridge.`<Bridge name>`.endpoint.path} to use Bridge endpoint path (i.e. for http://localhost:80/some/thing it will return /some/thing)
- ${bridge.`<Bridge name>`.processor.`<Processor name>`.id} to use actual "Processor id" linked to a specific Bridge
- ${bridge.`<Bridge name>`.id} to use actual "Bridge id"
- ${cloud-event.`<Cloud event id>`.id} to use "System cloud event id" (Cloud event id which is actually used for Cloud event invocation)
- ${data.`<Data id>`} to retrieve value stored in the context under this id
- ${env.`<System property name>`} to use a System property
- ${manager.authentication.token} to use a token for communication with Manager
- ${slack.channel.`<Channel name>`.name} to use actual channel name
- ${slack.channel.`<Channel name>`.webhook.url} to use a WebHook URL for sending message to the specified channel
- ${slack.token} to use a token for communication with Slack
- ${topic.`<Topic name>`} to use a unique Kafka topic name
- ${uuid.`<Uuid name>`} to use unique identifier. Useful to distinguish historical data produced by the same test for example.
- ${webhook.site.token.`<Token name>`} to use a webhook.site token with specified alias

## Test execution

### Local testing

If you deployed application locally (by [running manager and operator locally](../../../dev/README.md)) you can use [run-local-tests.sh](run-local-tests.sh) script to launch the tests with all local parameters set automatically.

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
mvn clean verify -Pcucumber -Devent-bridge.manager.url=<MANAGER_URL> -Dkeycloak.realm.url=<KEYCLOAK_URL> -Dbridge.client.id=<CLIENT_ID> -Dtest.credentials.file=localconfig.yaml
# Example:
# <CLIENT_ID>="cloud-services"
# <KEYCLOAK_URL>="https://sso.redhat.com/auth/realms/redhat-external"
```

### Remote system access configuration

To properly run the tests locally you need to specify environment variable to access webhook.site:
- `WEBHOOK_SITE_TOKEN` - UUID to be used for tests interacting with webhook.site

You can define this environment variable in localconfig.properties file or via the maven command, all environment variables are loaded before running the tests

Check the [localconfig-example.properties](localconfig-example.properties) file for an example of how to use it.

Also you need to create a localconfig.yaml file containing external system credentials and URLs for TNB framework. Check the [localconfig-example.yaml](localconfig-example.yaml) file for an example of how to use it.

### Keycloak token authentication

By Default test runs with those authentication parameters for Keycloak:

```xml
<bridge.client.id>kermit</bridge.client.id>
<bridge.client.secret>N5TW1EfuIcQsplRsLXJ1aE3DZZMPN5ZH</bridge.client.secret>
```

You can update the parameters needs to configure in `integration-tests/manager-integration-tests/manager-integration-tests-v2/pom.xml` or via the maven command.

### Performance testing

TO-DO: this section is a copy from v1 until a real performance tests are created for v2.

In order to be able to run the [performance tests scenarios](src/test/resources/performance-features) locally you will need to have the following installed on your machine:

- [Hyperfoil v0.24](https://hyperfoil.io/) - Benchmark framework for microservices (*)
- [webhook-perf-test](https://github.com/afalhambra/webhook-perf-test) - Dummy app to consume cloud events from webhook Processor

There is a dedicated Maven profile named `performance` located in `integration-tests/manager-integration-tests/manager-integration-tests-v2/pom.xml`. There you can see the following system properties needed 
to run these performance test scenarios:

```xml
<performance.webhook.url></performance.webhook.url>
<performance.hyperfoil.url></performance.hyperfoil.url>
```

Once you have `Hyperfoil` and the `webhook-perf-test` up and running you can run the performance tests by either replacing the properties mentioned above in the pom
file or by passing them as an argument via a Maven command. For example:
```bash
mvn clean verify -Pperformance -Dperformance.webhook.url=<WEBHOOK_PERF_URL> -Dperformance.hyperfoil.url=<HYPERFOIL_URL> -Devent-bridge.manager.url=<MANAGER_URL> -Dkeycloak.realm.url=<KEYCLOAK_URL>
```

> **_NOTE:_**  
>- Hyperfoil needs to be started up in a clustered mode (i.e. standalone mode will not work). For more information, please
refer to [Hyperfoil documentation](https://hyperfoil.io/)
>- `webhook-perf-test` needs to be deployed locally into your Minikube cluster. Use the script provided [here](https://github.com/afalhambra/webhook-perf-test/blob/main/bin/minikube/deploy.sh) for the same.