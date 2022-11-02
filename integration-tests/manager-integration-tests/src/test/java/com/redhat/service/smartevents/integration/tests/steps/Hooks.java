package com.redhat.service.smartevents.integration.tests.steps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.integration.tests.common.BridgeUtils;
import com.redhat.service.smartevents.integration.tests.common.Utils;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.AwsSqsResource;
import com.redhat.service.smartevents.integration.tests.resources.BridgeResource;
import com.redhat.service.smartevents.integration.tests.resources.ProcessorResource;
import com.redhat.service.smartevents.integration.tests.resources.kafka.KafkaResource;
import com.redhat.service.smartevents.integration.tests.resources.webhook.performance.WebhookPerformanceResource;
import com.redhat.service.smartevents.manager.v1.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v1.api.models.responses.ProcessorListResponse;

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
    private static final String DISABLE_FAIL_CLEANUP = Utils.getSystemProperty("fail.cleanup.disable");

    private TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    @BeforeAll(order = 0)
    public static void initializeLocalTestConfig() {
        final String filename = "localconfig.properties";
        File file = new File(filename);
        if (file.exists()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                Properties prop = System.getProperties();
                prop.load(inputStream);
                System.setProperties(prop);
            } catch (IOException e) {
                throw new RuntimeException("Failed to set properties.", e);
            }
        }

    }

    @BeforeAll(order = 1)
    public static void webhookPerformanceCleanUp() {
        if (WebhookPerformanceResource.isSpecified()) {
            WebhookPerformanceResource.deleteAll();
        }
    }

    @Before
    public void before(Scenario scenario) {
        context.setScenario(scenario);
        context.setStartTime(Instant.now());
    }

    @After
    public void cleanUp() {
        boolean disabledCleanup = Boolean.parseBoolean(DISABLE_CLEANUP);
        boolean disabledFailedCleanup = Boolean.parseBoolean(DISABLE_FAIL_CLEANUP);
        boolean scenarioFailed = context.getScenario().isFailed();
        if (disabledCleanup) {
            context.getScenario().log("Cleanup disabled, all resources stays in the cluster");
        } else if (disabledFailedCleanup && scenarioFailed) {
            context.getScenario().log("Failed scenario cleanup disabled and the scenario failed, all resources stays in the cluster");
        } else {
            // Delete AWS SQS queues
            cleanAwsSQSQueues();

            // Delete Kafka topics and related ACLs
            cleanKafkaTopics();

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

    private void cleanKafkaTopics() {
        for (String topic : context.allKafkaTopics()) {
            KafkaResource.deleteKafkaTopic(topic);
        }
    }

    private void cleanAwsSQSQueues() {
        for (String queueName : context.allSqsQueues()) {
            AwsSqsResource.deleteQueue(queueName);
        }
    }
}
