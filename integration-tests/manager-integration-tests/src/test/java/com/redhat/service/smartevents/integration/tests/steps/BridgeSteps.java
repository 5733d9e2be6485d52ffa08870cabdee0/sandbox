package com.redhat.service.smartevents.integration.tests.steps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.redhat.service.smartevents.infra.models.ManagedResourceStatus;
import com.redhat.service.smartevents.integration.tests.common.AwaitilityOnTimeOutHandler;
import com.redhat.service.smartevents.integration.tests.common.BridgeUtils;
import com.redhat.service.smartevents.integration.tests.common.Utils;
import com.redhat.service.smartevents.integration.tests.context.BridgeContext;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.BridgeResource;
import com.redhat.service.smartevents.manager.api.v1.models.responses.BridgeListResponse;
import com.redhat.service.smartevents.manager.api.v1.models.responses.BridgeResponse;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static com.redhat.service.smartevents.manager.services.v1.ProcessingErrorService.ENDPOINT_ERROR_HANDLER_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

public class BridgeSteps {

    private static final int BRIDGE_NAME_CREATE_RETRY = 20;
    private static final String ENDPOINT_URL_REGEX = "^(https?:\\/\\/[^/?#]+)([a-z0-9\\-._~%!$&'()*+,;=:@/]*)";
    private static final String NAME_REGEX = "\"name\": \"([^\"]+)\"";
    private static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX);
    private static final String NAME_REPLACE_TEMPLATE = "\"name\": \"%s\"";

    private TestContext context;

    public BridgeSteps(TestContext context) {
        this.context = context;
    }

    @Given("^the list of Bridge instances is failing with HTTP response code (\\d+)$")
    public void listOfBridgeInstancesIsFailingWithHTTPResponseCode(int responseCode) {
        BridgeResource.getBridgeListResponse(context.getManagerToken())
                .then()
                .statusCode(responseCode);
    }

    @When("^create a new Bridge \"([^\"]*)\" in cloud provider \"([^\"]*)\" and region \"([^\"]*)\"$")
    public void createNewBridge(String testBridgeName, String cloudProvider, String region) {
        createNewBridgeWithSupplier(testBridgeName, (context, systemBridgeName) -> BridgeResource.addBridge(context.getManagerToken(), systemBridgeName, cloudProvider, region));
    }

    @When("^create a new Bridge with body:$")
    public void createNewBridgeWithBody(String bridgeRequestJson) {
        String resolvedBridgeRequestJson = ContextResolver.resolveWithScenarioContext(context, bridgeRequestJson);
        Matcher nameMatcher = NAME_PATTERN.matcher(resolvedBridgeRequestJson);
        if (!nameMatcher.find()) {
            throw new RuntimeException("Can't find name in bridge request");
        }
        String testBridgeName = nameMatcher.group(1);
        createNewBridgeWithSupplier(testBridgeName, (context, systemBridgeName) -> {
            String updatedBridgeRequestJson = resolvedBridgeRequestJson.replaceAll(NAME_REGEX, String.format(NAME_REPLACE_TEMPLATE, systemBridgeName));
            try (InputStream resourceStream = new ByteArrayInputStream(updatedBridgeRequestJson.getBytes(StandardCharsets.UTF_8))) {
                return BridgeResource.addBridge(context.getManagerToken(), resourceStream);
            } catch (IOException e) {
                throw new UncheckedIOException("Error with inputstream", e);
            }
        });
    }

    private void createNewBridgeWithSupplier(String testBridgeName, BiFunction<TestContext, String, BridgeResponse> bridgeCreator) {
        String systemBridgeName = Utils.generateId("test-" + testBridgeName);
        int creationRetry = 1;
        while (creationRetry <= BRIDGE_NAME_CREATE_RETRY && isBridgeExisting(systemBridgeName)) {
            creationRetry++;
            systemBridgeName = Utils.generateId("test-" + testBridgeName);
        }
        if (isBridgeExisting(systemBridgeName)) {
            throw new RuntimeException(
                    "Cannot create and initiate a random bridge name correctly. Please cleanup the environment...");
        }

        BridgeResponse response = bridgeCreator.apply(context, systemBridgeName);
        assertThat(response.getName()).isEqualTo(systemBridgeName);
        assertThat(response.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
        assertThat(response.getEndpoint()).isNull();
        assertThat(response.getPublishedAt()).isNull();
        assertThat(response.getHref()).isNotNull();
        assertThat(response.getSubmittedAt()).isNotNull();

        context.newBridge(testBridgeName, response.getId(), systemBridgeName);
    }

    @When("^create a fake Bridge \"([^\"]*)\"$")
    public void createFakeBridge(String testBridgeName) {
        BridgeContext bridgeContext = context.newBridge(testBridgeName, Utils.generateId(testBridgeName),
                Utils.generateId("test-" + testBridgeName));
        bridgeContext.setDeleted(true);
    }

    @When("^update the Bridge \"([^\"]*)\" with body:$")
    public void updateBridge(String testBridgeName, String bridgeRequestJson) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String systemBridgeName = bridgeContext.getName();

        BridgeResponse response;
        String resolvedBridgeRequestJson = ContextResolver.resolveWithScenarioContext(context, bridgeRequestJson);
        String updatedBridgeRequestJson = resolvedBridgeRequestJson.replaceAll(NAME_REGEX, String.format(NAME_REPLACE_TEMPLATE, systemBridgeName));
        try (InputStream resourceStream = new ByteArrayInputStream(updatedBridgeRequestJson.getBytes(StandardCharsets.UTF_8))) {
            response = BridgeResource.updateBridge(context.getManagerToken(), bridgeContext.getId(), resourceStream);
        } catch (IOException e) {
            throw new RuntimeException("Error opening inputstream", e);
        }

        assertThat(response.getKind()).isEqualTo("Bridge");
        assertThat(response.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
    }

    @And("^the list of Bridge instances is containing the Bridge \"([^\"]*)\"$")
    public void listOfBridgeInstancesIsContainingBridge(String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        BridgeListResponse response = BridgeResource.getBridgeList(context.getManagerToken());

        assertThat(response.getItems()).anyMatch(b -> b.getId().equals(bridgeContext.getId()));
        BridgeResponse bridge = response.getItems().stream().filter(b -> b.getId().equals(bridgeContext.getId()))
                .findFirst().orElseThrow();
        assertThat(bridge.getName()).isEqualTo(bridgeContext.getName());
    }

    @And("^get Bridge \"([^\"]*)\" is failing with HTTP response code (\\d+)$")
    public void getBridgeIsFailingWithHTTPResponseCode(String testBridgeName, int responseCode) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        BridgeResource.getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId())
                .then()
                .statusCode(responseCode);
    }

    @And("^the Bridge \"([^\"]*)\" is existing with status \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void bridgeIsExistingWithStatusWithinMinutes(String testBridgeName, String status, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(() -> BridgeResource
                        .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId()).then().log().all()))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .failFast("Failed to create a Bridge",
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId())
                                .then()
                                .body("status", Matchers.not("failed")))
                .untilAsserted(
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId())
                                .then()
                                .body("status", Matchers.equalTo(status))
                                .body("endpoint", Matchers.containsString(bridgeContext.getId())));

        BridgeUtils.getOrRetrieveBridgeEventsEndpoint(context, testBridgeName);
    }

    @When("^delete the Bridge \"([^\"]*)\"$")
    public void deleteBridge(String testBridgeName) {
        BridgeResource.deleteBridge(context.getManagerToken(), context.getBridge(testBridgeName).getId());
        context.removeBridge(testBridgeName);
    }

    @When("^delete the Bridge \"([^\"]*)\" is failing with HTTP response code (\\d+)$")
    public void deleteBridgeIsFailingWithHTTPResponseCode(String testBridgeName, int responseCode) {
        BridgeResource.deleteBridgeResponse(context.getManagerToken(), context.getBridge(testBridgeName).getId())
                .then()
                .statusCode(responseCode);
    }

    @Then("^the Bridge \"([^\"]*)\" is not existing within (\\d+) (?:minute|minutes)$")
    public void bridgeIsNotExistingWithinMinutes(String testBridgeName, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(() -> BridgeResource
                        .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId()).then().log().all()))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> BridgeResource
                                .getBridgeDetailsResponse(context.getManagerToken(), bridgeContext.getId())
                                .then()
                                .statusCode(404));
    }

    @And("^the Bridge \"([^\"]*)\" has errorHandler of type \"([^\"]*)\" and parameters:$")
    public void bridgeHasErrorHandlerOfTypeAndParameters(String testBridgeName, String actionType, DataTable parametersDatatable) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        BridgeResponse response = BridgeResource
                .getBridgeDetails(context.getManagerToken(), bridgeContext.getId());
        assertThat(response.getErrorHandler().getType()).isEqualTo(actionType);
        parametersDatatable.asMap().forEach((key, value) -> {
            String parameterTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, value);
            assertThat(response.getErrorHandler().getParameter(key)).isEqualTo(parameterTextWithoutPlaceholders);
        });
    }

    @And("^the Bridge \"([^\"]*)\" has a polling error handler endpoint$")
    public void bridgeHasPollingErrorHandlerEndpoint(String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        BridgeResponse response = BridgeResource
                .getBridgeDetails(context.getManagerToken(), bridgeContext.getId());
        assertThat(response.getErrorHandler().getType()).isEqualTo(ENDPOINT_ERROR_HANDLER_TYPE);
        assertThat(BridgeUtils.getOrRetrieveBridgePollingErrorHandlerEndpoint(context, testBridgeName)).matches(ENDPOINT_URL_REGEX);
    }

    @Then("^the polling error handler endpoint of the Bridge \"([^\"]*)\" contains message \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void pollingErrorHandlerContainsMessage(String testBridgeName, String errorMessage, int timeoutMinutes) {
        String resolvedErrorMessage = ContextResolver.resolveWithScenarioContext(context, errorMessage);
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String errorHandlerEndpoint = bridgeContext.getErrorHandlerEndpoint();
        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(() -> BridgeResource
                        .getBridgeErrorHandlerResponse(context.getManagerToken(), errorHandlerEndpoint).then().log().all()))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(BridgeResource.getBridgeErrorHandlerResponse(context.getManagerToken(), errorHandlerEndpoint)
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .extract()
                        .jsonPath().getList("items")).anyMatch(
                                item -> ((Map<String, Object>) ((Map<String, Object>) item).get("payload")).get("data")
                                        .toString().contains(resolvedErrorMessage)));
    }

    private boolean isBridgeExisting(String bridgeName) {
        return BridgeResource.getBridgeList(context.getManagerToken()).getItems().stream()
                .anyMatch(b -> b.getName().equals(bridgeName));
    }

    @And("^the polling error handler endpoint of the Bridge \"([^\"]*)\" has only (\\d+) error (?:message|messages) within (\\d+) (?:minute|minutes)$")
    public void thePollingErrorHandlerContainsNErrorMessages(String testBridgeName, int numberErrors, int timeoutMinutes) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);
        String errorHandlerEndpoint = bridgeContext.getErrorHandlerEndpoint();
        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(() -> BridgeResource
                        .getBridgeErrorHandlerResponse(context.getManagerToken(), errorHandlerEndpoint).then().log().all()))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(BridgeResource.getBridgeErrorHandlerResponse(context.getManagerToken(), errorHandlerEndpoint)
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .extract()
                        .jsonPath().getList("items")).hasSize(numberErrors));
    }
}
