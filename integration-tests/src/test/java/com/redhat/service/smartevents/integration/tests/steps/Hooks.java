package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.integration.tests.common.BridgeUtils;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.BridgeResource;
import com.redhat.service.smartevents.integration.tests.resources.ProcessorResource;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteResource;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorListResponse;

import io.cucumber.core.logging.Logger;
import io.cucumber.core.logging.LoggerFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
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

    @BeforeAll
    public static void webhookSiteRequestHistoryIsCleared() {
        final LocalDate yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        WebhookSiteResource.requests()
                .stream()
                .filter(request -> {
                    final LocalDate requestCreatedAt = request.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return yesterday.isAfter(requestCreatedAt);
                })
                .forEach(request -> WebhookSiteResource.deleteRequest(request));
    }

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
                                Awaitility.await()
                                        .atMost(Duration.ofMinutes(4))
                                        .pollInterval(Duration.ofSeconds(5))
                                        .until(() -> BridgeResource.getBridgeList(token).getItems().stream().noneMatch(b -> b.getId().equals(bridgeId)));
                            } catch (Exception e) {
                                LOGGER.warn(e, () -> "Unable to delete bridge with id " + bridgeId);
                            }
                        default:
                            break;
                    }
                });
    }
}
