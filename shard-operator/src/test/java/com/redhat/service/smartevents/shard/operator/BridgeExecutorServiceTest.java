package com.redhat.service.smartevents.shard.operator;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.shard.operator.resources.camel.CamelIntegration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.Constants;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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

        Action a = new Action();
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, "myTopic");
        a.setMapParameters(params);

        String spec = "{\n" +
                "      \"flow\": {\n" +
                "        \"from\": {\n" +
                "          \"uri\": \"rhose\",\n" +
                "          \"steps\": [\n" +
                "            {\n" +
                "              \"to\": \"mySlackAction\"\n" +
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
                                                                          a,
                                                                          null,
                                                                          camelProcessing);


        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO(processorDefinition);

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Then
        CamelIntegration camelIntegration = kubernetesClient
                .resources(CamelIntegration.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(CamelIntegration.resolveResourceName(dto.getId()))
                .get();

        assertThat(camelIntegration).isNotNull();

//        assertThat(camelIntegration.getSpec().getOwner()).isEqualTo(dto.getOwner());
//
//        Secret secret = fetchBridgeExecutorSecret(dto);
//        assertThat(secret).isNotNull();
//        assertThat(secret.getMetadata().getName()).isEqualTo(camelIntegration.getMetadata().getName());
//        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR)).isNotEmpty();
//        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR)).isNotEmpty();
//        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR)).isNotEmpty();
//        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_SECURITY_PROTOCOL_ENV_VAR)).isNotEmpty();
//        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR)).isNotEmpty();
//        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_ERROR_STRATEGY_ENV_VAR)).isNotEmpty();
//        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_ERROR_TOPIC_ENV_VAR)).isNotEmpty();
//        assertThat(secret.getData().get(GlobalConfigurationsConstants.KAFKA_GROUP_ID_ENV_VAR)).isNotEmpty();
    }

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testBridgeExecutorCreationTriggersController() {
        // Given
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Then
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
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
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
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
        deployment = bridgeExecutorService.fetchOrCreateBridgeExecutorDeployment(fetchBridgeIngress(dto), fetchBridgeExecutorSecret(dto));
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isEqualTo(TestSupport.EXECUTOR_IMAGE);
    }

    @Test
    public void testBridgeIngressDeletion() {
        // Given
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();

        // When
        bridgeExecutorService.createBridgeExecutor(dto);
        bridgeExecutorService.deleteBridgeExecutor(dto);

        // Then
        BridgeExecutor bridgeExecutor = kubernetesClient
                .resources(BridgeExecutor.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeExecutor.resolveResourceName(dto.getId()))
                .get();
        assertThat(bridgeExecutor).isNull();
    }

    private BridgeExecutor fetchBridgeIngress(ProcessorDTO dto) {
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
