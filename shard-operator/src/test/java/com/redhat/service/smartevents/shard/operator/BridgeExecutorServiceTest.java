package com.redhat.service.smartevents.shard.operator;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.shard.operator.monitoring.ServiceMonitorService;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.camel.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.utils.Constants;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.shard.operator.utils.AwaitilityUtil.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class BridgeExecutorServiceTest {

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    ServiceMonitorService monitorService;

    @InjectMock
    ManagerClient managerClient;

    @BeforeEach
    public void setup() {
        // Kubernetes Server must be cleaned up at startup of every test.
        kubernetesResourcePatcher.cleanUp();
    }

    @Test
    public void testBridgeExecutorCreation() {
        // Given
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Then
        BridgeExecutor bridgeExecutor = kubernetesClient
                .resources(BridgeExecutor.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeExecutor.resolveResourceName(dto.getId()))
                .get();
        assertThat(bridgeExecutor).isNotNull();
        assertThat(bridgeExecutor.getSpec().getOwner()).isEqualTo(dto.getOwner());

        Secret secret = fetchBridgeExecutorSecret(dto);
        assertThat(secret).isNotNull();
        assertThat(secret.getMetadata().getName()).isEqualTo(bridgeExecutor.getMetadata().getName());
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR)).isNotEmpty();
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR)).isNotEmpty();
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR)).isNotEmpty();
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_SECURITY_PROTOCOL_ENV_VAR)).isNotEmpty();
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR)).isNotEmpty();
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_ERROR_STRATEGY_ENV_VAR)).isNotEmpty();
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_ERROR_TOPIC_ENV_VAR)).isNotEmpty();
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_GROUP_ID_ENV_VAR)).isNotEmpty();
    }

    @Test
    public void testCamelResourceCreated() {

        ProcessorDTO dto = processorDTOWithCamelProcessing();

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Then
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            // The deployment is deployed by the controller

                            // Then
                            String camelIntegrationName = CamelIntegration.resolveResourceName(dto.getId());

                            CamelIntegration camelIntegration = kubernetesClient
                                    .resources(CamelIntegration.class)
                                    .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                                    .withName(camelIntegrationName)
                                    .get();

                            assertThat(camelIntegration).isNotNull();

                            System.out.println("+++++++" + camelIntegration);

                            ObjectNode camelIntegrationSpec = camelIntegration.getSpec();

                            List<JsonNode> camelIntegrationFlows = camelIntegrationSpec.findValues("flows");

                            JsonNode flow = camelIntegrationFlows.get(0);

                            JsonNode camelIntegrationFrom = flow.get(0).get("from");

                            assertThat(camelIntegrationFrom.get("uri").asText()).isEqualTo(String.format("kafka:ob-%s", TestSupport.BRIDGE_ID));
                            ObjectNode parameters = (ObjectNode) camelIntegrationFrom.get("parameters");

                            assertThat(parameters.get("brokers").asText()).isEqualTo("mytestkafka:9092");
                            assertThat(parameters.get("securityProtocol").asText()).isEqualTo("SASL_SSL");
                            assertThat(parameters.get("saslMechanism").asText()).isEqualTo("PLAIN");
                            assertThat(parameters.get("saslJaasConfig").asText())
                                    .isEqualTo("org.apache.kafka.common.security.plain.PlainLoginModule required username='client-id' password='testsecret';");
                            assertThat(parameters.get("maxPollRecords").intValue()).isEqualTo(5000);
                            assertThat(parameters.get("consumersCount").intValue()).isEqualTo(1);
                            assertThat(parameters.get("seekTo").asText()).isEqualTo("beginning");
                            assertThat(parameters.get("groupId").asText()).isEqualTo("kafkaGroup");

                            JsonNode camelIntegrationTo = camelIntegrationFrom.get("steps").iterator().next();

                            JsonNode to = camelIntegrationTo.get("to");
                            assertThat(to.get("uri").asText()).isEqualTo("kafka:kafkaOutputTopic");

                            ObjectNode toParameters = (ObjectNode) to.get("parameters");

                            assertThat(toParameters.get("brokers").asText()).isEqualTo("mytestkafka:9092");
                            assertThat(toParameters.get("securityProtocol").asText()).isEqualTo("SASL_SSL");
                            assertThat(toParameters.get("saslMechanism").asText()).isEqualTo("PLAIN");
                            assertThat(toParameters.get("saslJaasConfig").asText())
                                    .isEqualTo("org.apache.kafka.common.security.plain.PlainLoginModule required username='client-id' password='testsecret';");
                            assertThat(toParameters.get("maxPollRecords").intValue()).isEqualTo(5000);
                            assertThat(toParameters.get("consumersCount").intValue()).isEqualTo(1);
                            assertThat(toParameters.get("seekTo").asText()).isEqualTo("beginning");
                            assertThat(toParameters.get("groupId").asText()).isEqualTo("kafkaGroup");

                        });

    }

    @Test
    public void testCamelResourceDelete() {

        ProcessorDTO dto = processorDTOWithCamelProcessing();

        // When
        bridgeExecutorService.createBridgeExecutor(dto);
        bridgeExecutorService.deleteBridgeExecutor(dto);

        // Then
        CamelIntegration camelIntegration = kubernetesClient
                .resources(CamelIntegration.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(CamelIntegration.resolveResourceName(dto.getId()))
                .get();
        assertThat(camelIntegration).isNull();
    }

    private ProcessorDTO processorDTOWithCamelProcessing() {
        Action resolvedAction1 = createKafkaAction("mySlackAction", "kafkaOutputTopic");
        Action resolvedAction2 = createKafkaAction("otherAction", "doNotUse");

        String spec = "{\n" +
                "      \"flow\": {\n" +
                "        \"from\": {\n" +
                "          \"uri\": \"rhose\",\n" +
                "          \"steps\": [\n" +
                "            {\n" +
                "              \"to\": { \"uri\" : \"mySlackAction\" } \n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      }\n" +
                "    }";
        ObjectNode flowSpec = null;
        try {
            flowSpec = (ObjectNode) objectMapper.readTree(spec);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Processing camelProcessing = new Processing("cameldsl_0.1", flowSpec);

        ProcessorDefinition processorDefinition = new ProcessorDefinition(Collections.emptySet(),
                "",
                null,
                null,
                camelProcessing,
                Arrays.asList(resolvedAction1, resolvedAction2),
                Arrays.asList(resolvedAction1, resolvedAction2));

        return TestSupport.newRequestedProcessorDTO(processorDefinition);
    }

    private Action createKafkaAction(String name, String topic) {
        Action resolvedAction = new Action();
        resolvedAction.setType(SlackAction.TYPE);
        resolvedAction.setName(name);

        Map<String, String> resolvedActionParams = new HashMap<>();
        resolvedActionParams.put(KafkaTopicAction.TOPIC_PARAM, topic);
        resolvedAction.setMapParameters(resolvedActionParams);
        return resolvedAction;
    }

    @Test
    public void testBridgeExecutorCreationTriggersController() {
        // Given
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Then
        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    // The deployment is deployed by the controller
                    Deployment deployment = kubernetesClient.apps().deployments()
                            .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                            .withName(BridgeExecutor.resolveResourceName(dto.getId()))
                            .get();
                    assertThat(deployment).isNotNull();
                    assertThat(deployment.getSpec().getProgressDeadlineSeconds()).isEqualTo(60);
                    List<EnvVar> environmentVariables = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
                    assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.CUSTOMER_ID_CONFIG_ENV_VAR)).findFirst().get().getValue().length())
                            .isGreaterThan(0);
                    assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.BRIDGE_EXECUTOR_PROCESSOR_DEFINITION_ENV_VAR)).findFirst().get().getValue().length())
                            .isGreaterThan(0);
                    assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.BRIDGE_EXECUTOR_WEBHOOK_SSO_ENV_VAR)).findFirst().get().getValue()
                            .length())
                                    .isGreaterThan(0);
                    assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.BRIDGE_EXECUTOR_WEBHOOK_CLIENT_ID_ENV_VAR)).findFirst().get().getValue()
                            .length())
                                    .isGreaterThan(0);
                    assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.BRIDGE_EXECUTOR_WEBHOOK_CLIENT_SECRET_ENV_VAR)).findFirst().get().getValue()
                            .length())
                                    .isGreaterThan(0);

                });
    }

    @Test
    public void testFetchOrCreateBridgeExecutorDeploymentRedeployment() {
        // Given
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        String patchedImage = TestSupport.EXECUTOR_IMAGE + "-patched";

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Wait until deployment is created by the controller.
        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    // The deployment is deployed by the controller
                    Deployment deployment = fetchBridgeExecutorDeployment(dto);
                    assertThat(deployment).isNotNull();
                });

        // Patch the deployment and replace
        Deployment deployment = fetchBridgeExecutorDeployment(dto);
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(patchedImage);
        kubernetesClient.apps().deployments().inNamespace(deployment.getMetadata().getNamespace()).createOrReplace(deployment);

        // Then
        deployment = bridgeExecutorService.fetchOrCreateBridgeExecutorDeployment(fetchBridgeExecutor(dto), fetchBridgeExecutorSecret(dto));
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isEqualTo(TestSupport.EXECUTOR_IMAGE);
    }

    @Test
    public void testBridgeExecutorDeletion() {
        // Given
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();

        // When
        bridgeExecutorService.createBridgeExecutor(dto);
        bridgeExecutorService.deleteBridgeExecutor(dto);

        // Then
        BridgeExecutor bridgeExecutor = fetchBridgeExecutor(dto);
        assertThat(bridgeExecutor).isNull();
    }

    @Test
    public void testBridgeExecutorCreationWhenSpecAlreadyExistsAsProvisioning() {
        // Given a PROVISIONING Processor
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        dto.setStatus(PROVISIONING);

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Then
        // Manager is not notified
        assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
        verifyNoInteractions(managerClient);

        // Re-try creation
        bridgeExecutorService.createBridgeExecutor(dto);

        // Manager is still not notified as the BridgeExecutor is not yet ready
        assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
        verifyNoInteractions(managerClient);
    }

    @Test
    public void testBridgeExecutorCreationWhenSpecAlreadyExistsAsReady() {
        // Given a PROVISIONING Processor
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        dto.setStatus(PROVISIONING);

        // Mock the presence of Prometheus Custom Resource
        ServiceMonitor serviceMonitor = mock(ServiceMonitor.class);
        when(monitorService.fetchOrCreateServiceMonitor(any(BridgeExecutor.class),
                any(Service.class),
                eq(BridgeExecutor.COMPONENT_NAME)))
                        .thenReturn(Optional.of(serviceMonitor));

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Then
        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    BridgeExecutor bridgeExecutor = fetchBridgeExecutor(dto);
                    kubernetesResourcePatcher.patchReadyDeploymentAsReady(bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());
                });

        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    BridgeExecutor bridgeExecutor = fetchBridgeExecutor(dto);
                    kubernetesResourcePatcher.patchReadyService(bridgeExecutor.getMetadata().getName(), bridgeExecutor.getMetadata().getNamespace());
                });

        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    BridgeExecutor bridgeExecutor = fetchBridgeExecutor(dto);
                    assertThat(bridgeExecutor.getStatus().isReady()).isTrue();
                });

        ArgumentCaptor<ProcessorManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ProcessorManagedResourceStatusUpdateDTO.class);

        // When the reconciliation completes the DTO remains in PROVISIONING, but we've notified the Manager that it is READY
        assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
        verify(managerClient, times(1)).notifyProcessorStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> {
            assertThat(d.getId()).isEqualTo(dto.getId());
            assertThat(d.getCustomerId()).isEqualTo(dto.getCustomerId());
            assertThat(d.getBridgeId()).isEqualTo(dto.getBridgeId());
            assertThat(d.getStatus()).isEqualTo(READY);
        });

        // Re-try creation
        bridgeExecutorService.createBridgeExecutor(dto);

        verify(managerClient, times(2)).notifyProcessorStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> {
            assertThat(d.getId()).isEqualTo(dto.getId());
            assertThat(d.getCustomerId()).isEqualTo(dto.getCustomerId());
            assertThat(d.getBridgeId()).isEqualTo(dto.getBridgeId());
            assertThat(d.getStatus()).isEqualTo(READY);
        });
    }

    private BridgeExecutor fetchBridgeExecutor(ProcessorDTO dto) {
        return kubernetesClient
                .resources(BridgeExecutor.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private Deployment fetchBridgeExecutorDeployment(ProcessorDTO dto) {
        return kubernetesClient.apps().deployments()
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeExecutor.resolveResourceName(dto.getId()))
                .get();
    }

    private Secret fetchBridgeExecutorSecret(ProcessorDTO dto) {
        return kubernetesClient.secrets()
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeExecutor.resolveResourceName(dto.getId()))
                .get();
    }
}
