package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.integration.tests.context.BridgeContext;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.BridgeResource;
import com.redhat.service.bridge.integration.tests.resources.ProcessorResource;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber hooks for setup and cleanup
 */
public class Hooks {

    private TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    // Enable this if you need to see all the requests / responses which are failing
    // @BeforeAll
    // public static void beforeAll() {
    //     RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    // }

    @Before
    public void before(Scenario scenario) {
        this.context.setScenario(scenario);
    }

    @After
    public void cleanUp() {
        System.out.println("Started cleanup");
        // Remove all bridges/processors created
        context.getAllBridgeNames().stream().map(context::getBridge).map(BridgeContext::getBridgeId)
                .forEach(bridgeId -> {
                    System.out.println("Clean bridge with id " + bridgeId);
                    BridgeResponse bridge = BridgeResource.getBridgeDetails(context.getManagerToken(), bridgeId);
                    if (bridge.getStatus() == BridgeStatus.READY) {
                        ProcessorListResponse processorList = ProcessorResource.getProcessorList(
                                context.getManagerToken(),
                                bridgeId);
                        if (processorList.getSize() > 0) {
                            processorList.getItems().stream().forEach(
                                    p -> ProcessorResource.deleteProcessor(context.getManagerToken(), bridgeId,
                                            p.getId()));
                            Awaitility.await()
                                    .atMost(Duration.ofMinutes(2))
                                    .pollInterval(Duration.ofSeconds(5))
                                    .until(() -> ProcessorResource.getProcessorList(context.getManagerToken(), bridgeId)
                                            .getSize() == 0);
                        }
                        BridgeResource.deleteBridge(context.getManagerToken(), bridgeId);
                    }
                });
    }
}
