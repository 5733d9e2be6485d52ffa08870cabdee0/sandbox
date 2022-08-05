package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.shard.operator.monitoring.ServiceMonitorService;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    public void testBridgeExecutorCreationWhenSpecAlreadyExists() {
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
