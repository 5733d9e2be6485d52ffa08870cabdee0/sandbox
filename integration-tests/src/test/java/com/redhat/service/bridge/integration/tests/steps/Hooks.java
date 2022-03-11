package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;
import java.util.Optional;

import org.awaitility.Awaitility;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.BridgeResource;
import com.redhat.service.bridge.integration.tests.resources.ProcessorResource;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;

import io.cucumber.core.logging.Logger;
import io.cucumber.core.logging.LoggerFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber hooks for setup and cleanup
 */
public class Hooks {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hooks.class);

    private TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    // Enable this if you need to see all the requests / responses which are failing
    // @BeforeAll
    // public static void beforeAll() {
    // RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    // }

    @Before
    public void before(Scenario scenario) {
        this.context.setScenario(scenario);
    }

    @After
    public void cleanUp() {
        String token = Optional.ofNullable(context.getManagerToken()).orElse(BridgeUtils.retrieveBridgeToken());
        // Remove all bridges/processors created
        context.getAllBridges().values()
                .stream()
                .filter(bridgeContext -> !bridgeContext.isDeleted())
                .forEach(bridgeContext -> {
                    final String bridgeId = bridgeContext.getId();
                    BridgeResponse bridge = BridgeResource.getBridgeDetails(token, bridgeId);
                    if (bridge.getStatus() == ManagedResourceStatus.READY) {
                        ProcessorListResponse processorList = ProcessorResource.getProcessorList(
                                token,
                                bridgeId);
                        if (processorList.getSize() > 0) {
                            processorList.getItems().stream().forEach(
                                    p -> ProcessorResource.deleteProcessor(token, bridgeId,
                                            p.getId()));
                            Awaitility.await()
                                    .atMost(Duration.ofMinutes(2))
                                    .pollInterval(Duration.ofSeconds(5))
                                    .until(() -> ProcessorResource.getProcessorList(token, bridgeId)
                                            .getSize() == 0);
                        }
                    }
                    switch (bridge.getStatus()) {
                        case ACCEPTED:
                        case PROVISIONING:
                        case READY:
                        case FAILED:
                            try {
                                BridgeResource.deleteBridge(token, bridgeId);

                                // Wait until the Bridge is deleted before finishing the scenario. The reason is that creating a Bridge while another Bridge is deleted seems to cause various transaction errors.
                                // TODO: remove once https://github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox/pull/431 is merged, which should hopefully address the issue
                                Awaitility.await()
                                        .atMost(Duration.ofMinutes(2))
                                        .pollInterval(Duration.ofSeconds(1))
                                        .untilAsserted(
                                                () -> BridgeResource
                                                        .getBridgeDetailsResponse(context.getManagerToken(), bridgeId)
                                                        .then()
                                                        .statusCode(404));
                            } catch (Exception e) {
                                LOGGER.warn(e, () -> "Unable to delete bridge with id " + bridgeId);
                            }
                        default:
                            break;
                    }
                });
    }
}
