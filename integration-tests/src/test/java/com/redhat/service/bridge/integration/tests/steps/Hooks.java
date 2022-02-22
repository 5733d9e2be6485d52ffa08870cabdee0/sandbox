package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.BridgeResource;
import com.redhat.service.bridge.integration.tests.resources.ProcessorResource;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks {

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
        String token = BridgeUtils.retrieveBridgeToken();
        String bridgeId = context.getBridgeId();
        if (BridgeResource.getBridgeList(token).getItems().stream()
                .anyMatch(b -> b.getId().equals(context.getBridgeId()))) {
            BridgeResponse bridge = BridgeResource.getBridgeDetails(context.getManagerToken(), bridgeId);
            if (bridge.getStatus() == BridgeStatus.READY) {
                if (ProcessorResource.getProcessorList(token, bridgeId).getSize() > 0) {
                    ProcessorResource.getProcessorList(token, bridgeId).getItems().stream()
                            .forEach(p -> ProcessorResource.deleteProcessor(token, bridgeId, p.getId()));
                    Awaitility.await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(5))
                            .until(() -> ProcessorResource.getProcessorList(token, bridgeId).getSize() == 0);
                }
            }
            BridgeResource.deleteBridge(token, bridgeId);
        }
    }
}
