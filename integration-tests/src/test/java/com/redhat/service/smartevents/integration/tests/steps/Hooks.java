package com.redhat.service.smartevents.integration.tests.steps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.integration.tests.common.BridgeUtils;
import com.redhat.service.smartevents.integration.tests.common.Utils;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.BridgeResource;
import com.redhat.service.smartevents.integration.tests.resources.ProcessorResource;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteQuerySorting;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteResource;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorListResponse;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber hooks for setup and cleanup
 */
public class Hooks {

    private static final String DISABLE_CLEANUP = Utils.getSystemProperty("cleanup.disable");

    private TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    @BeforeAll
    public static void initializeLocalTestConfig() {
        final String filename = "localconfig.properties";
        File file = new File(filename);
        if (file.exists()) {
            try {
                InputStream inputStream = Hooks.class.getClassLoader().getResourceAsStream(filename);
                Properties prop = System.getProperties();
                prop.load(inputStream);
                System.setProperties(prop);
            } catch (IOException e) {
                throw new RuntimeException("Failed to set properties.", e);
            }
        }
    }

    @BeforeAll
    public static void webhookSiteRequestHistoryIsCleared() {
        final LocalDate yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        WebhookSiteResource.requests(WebhookSiteQuerySorting.OLDEST)
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
        if (!Boolean.parseBoolean(DISABLE_CLEANUP)) {
            String token = Optional.ofNullable(context.getManagerToken()).orElse(BridgeUtils.retrieveBridgeToken());
            // Remove all bridges/processors created
            context.getAllBridges().values()
                    .parallelStream()
                    .filter(bridgeContext -> !bridgeContext.isDeleted())
                    .forEach(bridgeContext -> {
                        final String bridgeId = bridgeContext.getId();
                        BridgeResponse bridge = BridgeResource.getBridgeDetails(token, bridgeId);
                        if (bridge.getStatus() == ManagedResourceStatus.READY) {
                            ProcessorListResponse processorList = ProcessorResource.getProcessorList(
                                    token,
                                    bridgeId);
                            if (processorList.getSize() > 0) {
                                processorList.getItems().parallelStream().forEach(
                                        p -> {
                                            String processorId = p.getId();
                                            ProcessorResource.deleteProcessor(token, bridgeId, processorId);
                                            Awaitility.await()
                                                    .atMost(Duration.ofMinutes(4))
                                                    .pollInterval(Duration.ofSeconds(5))
                                                    .untilAsserted(
                                                            () -> assertThat(ProcessorResource.getProcessorList(token, bridgeId).getItems())
                                                                    .as("waiting until Processor `%s` of the Bridge `%s` is deleted", processorId, bridgeId)
                                                                    .noneMatch(processor -> Objects.equals(processor.getId(), processorId)));
                                        });
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
                                            .untilAsserted(
                                                    () -> assertThat(BridgeResource.getBridgeList(token).getItems()).as("waiting until Bridge `%s` is deleted", bridgeId)
                                                            .noneMatch(b -> Objects.equals(b.getId(), bridgeId)));
                                } catch (Exception e) {
                                    throw new RuntimeException("Unable to delete bridge with id " + bridgeId, e);
                                }
                            default:
                                break;
                        }
                    });
        }
    }
}
