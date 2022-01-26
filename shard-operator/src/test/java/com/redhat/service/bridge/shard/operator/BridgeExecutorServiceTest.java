package com.redhat.service.bridge.shard.operator;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.Constants;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.bridge.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(KeycloakResource.class)
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
    }

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
                            List<EnvVar> environmentVariables = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
                            assertThat(
                                    environmentVariables.stream().filter(x -> x.getName().equals(GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR)).findFirst().get().getValue().length())
                                            .isGreaterThan(0);
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR)).findFirst().get().getValue().length())
                                    .isGreaterThan(0);
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR)).findFirst().get().getValue().length())
                                    .isGreaterThan(0);
                            assertThat(
                                    environmentVariables.stream().filter(x -> x.getName().equals(GlobalConfigurationsConstants.KAFKA_SECURITY_PROTOCOL_ENV_VAR)).findFirst().get().getValue().length())
                                            .isGreaterThan(0);
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(GlobalConfigurationsConstants.KAFKA_GROUP_ID_ENV_VAR)).findFirst().get().getValue().length())
                                    .isGreaterThan(0);
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.BRIDGE_EXECUTOR_PROCESSOR_DEFINITION_ENV_VAR)).findFirst().get().getValue().length())
                                    .isGreaterThan(0);
                            assertThat(environmentVariables.stream().filter(x -> x.getName().equals(Constants.BRIDGE_EXECUTOR_WEBHOOK_TECHNICAL_BEARER_TOKEN_ENV_VAR)).findFirst().get().getValue()
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
        deployment = bridgeExecutorService.fetchOrCreateBridgeExecutorDeployment(fetchBridgeIngress(dto));
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isEqualTo(TestSupport.EXECUTOR_IMAGE);
    }

    @Test
    public void testBridgeIngressDeletion() {
        // Given
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        // Then
        BridgeIngress bridgeIngress = kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeExecutor.resolveResourceName(dto.getId()))
                .get();
        assertThat(bridgeIngress).isNull();
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
}
