package com.redhat.service.smartevents.shard.operator.v1;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v1.api.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.v1.monitoring.ServiceMonitorService;
import com.redhat.service.smartevents.shard.operator.v1.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.v1.utils.Constants;
import com.redhat.service.smartevents.shard.operator.v1.utils.KubernetesResourcePatcher;
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

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.shard.operator.v1.utils.AwaitilityUtil.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
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

    @InjectMock
    ServiceMonitorService monitorService;

    @InjectMock
    ManagerClient managerClient;

    @InjectMock
    TemplateProvider templateProvider;

    @InjectMock
    OperatorMetricsService metricsService;

    @BeforeEach
    public void setup() {
        // Kubernetes Server must be cleaned up at startup of every test.
        kubernetesResourcePatcher.cleanUp();

        when(templateProvider.loadBridgeExecutorSecretTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeExecutorDeploymentTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeExecutorServiceTemplate(any(), any())).thenCallRealMethod();

        // Far from ideal... but each test assumes there are no other BridgeExecutor instances in existence.
        // Unfortunately, however, some tests only check that provisioning either progressed to a certain
        // point or failed completely. There is therefore a good chance there's an incomplete BridgeExecutor
        // in k8s when a subsequent test starts. This leads to non-deterministic behaviour of tests.
        // This ensures each test has a "clean" k8s environment.
        await(Duration.ofMinutes(1),
                Duration.ofSeconds(10),
                () -> assertThat(kubernetesClient.resources(BridgeExecutor.class).inAnyNamespace().list().getItems().isEmpty()).isTrue());
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
    public void testBridgeExecutorCreationWhenSpecAlreadyExistsAsProvisioning() {
        // Given a PROVISIONING Processor
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        dto.setStatus(PROVISIONING);

        // When
        bridgeExecutorService.createBridgeExecutor(dto);
        waitUntilBridgeExecutorSecretAvailable(dto);

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

        deployExecutorSuccessfully(dto);

        // Re-try creation
        bridgeExecutorService.createBridgeExecutor(dto);

        ArgumentCaptor<ProcessorManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ProcessorManagedResourceStatusUpdateDTO.class);

        verify(managerClient, times(2)).notifyProcessorStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> assertProcessorManagedResourceStatusUpdateDTOUpdate(d,
                dto.getId(),
                dto.getCustomerId(),
                dto.getBridgeId(),
                READY));
    }

    private void deployExecutorSuccessfully(ProcessorDTO dto) {
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
        updateDTO.getAllValues().forEach((d) -> assertProcessorManagedResourceStatusUpdateDTOUpdate(d,
                dto.getId(),
                dto.getCustomerId(),
                dto.getBridgeId(),
                READY));
        verify(metricsService).onOperationComplete(any(BridgeExecutor.class), eq(MetricsOperation.CONTROLLER_RESOURCE_PROVISION));
    }

    private void assertProcessorManagedResourceStatusUpdateDTOUpdate(ProcessorManagedResourceStatusUpdateDTO update,
            String expectedId,
            String expectedCustomerId,
            String expectedBridgeId,
            ManagedResourceStatus expectedStatus) {
        assertThat(update.getId()).isEqualTo(expectedId);
        assertThat(update.getCustomerId()).isEqualTo(expectedCustomerId);
        assertThat(update.getBridgeId()).isEqualTo(expectedBridgeId);
        assertThat(update.getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void testBridgeExecutorCreationWhenSpecAlreadyExistsAsFailed() {
        // Given a PROVISIONING Processor
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        dto.setStatus(PROVISIONING);

        // By not mocking ServiceMonitor the Prometheus Custom Resource check will fail
        // causing the Controller to update the BridgeExecutor status to FAILED.

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
                    assertThat(bridgeExecutor.getStatus().isReady()).isFalse();
                });

        ArgumentCaptor<ProcessorManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ProcessorManagedResourceStatusUpdateDTO.class);

        // When the reconciliation completes the DTO remains in PROVISIONING, but we've notified the Manager that it has FAILED
        assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
        verify(managerClient, atLeastOnce()).notifyProcessorStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> assertProcessorManagedResourceStatusUpdateDTOUpdate(d,
                d.getId(),
                d.getCustomerId(),
                d.getBridgeId(),
                FAILED));

        // Re-try creation
        bridgeExecutorService.createBridgeExecutor(dto);

        // Since the DTO remained in PROVISIONING the manager is not notified again.
        verify(managerClient, atLeastOnce()).notifyProcessorStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> assertProcessorManagedResourceStatusUpdateDTOUpdate(d,
                dto.getId(),
                dto.getCustomerId(),
                dto.getBridgeId(),
                FAILED));
    }

    @Test
    public void testBridgeExecutorCreationWhenSpecAlreadyExistsAsFailedMaxRetries() {
        // Given a PROVISIONING Processor
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        dto.setStatus(PROVISIONING);

        // Mock an exception being thrown by the controller
        // k8s max-reties is set to 1 in application.properties, overriding the default
        // See https://github.com/quarkiverse/quarkus-operator-sdk/issues/380#issuecomment-1211343353
        reset(templateProvider);
        when(templateProvider.loadBridgeExecutorSecretTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeExecutorDeploymentTemplate(any(), any())).thenThrow(new InternalPlatformException("template-provider-error"));

        // When
        bridgeExecutorService.createBridgeExecutor(dto);

        ArgumentCaptor<ProcessorManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ProcessorManagedResourceStatusUpdateDTO.class);

        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // When the reconciliation completes the DTO remains in PROVISIONING, but we've notified the Manager that it is FAILED
                    assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
                    verify(managerClient).notifyProcessorStatusChange(updateDTO.capture());
                    updateDTO.getAllValues().forEach((d) -> assertProcessorManagedResourceStatusUpdateDTOUpdate(d,
                            dto.getId(),
                            dto.getCustomerId(),
                            dto.getBridgeId(),
                            FAILED));
                    verify(metricsService).onOperationFailed(any(BridgeExecutor.class), eq(MetricsOperation.CONTROLLER_RESOURCE_PROVISION));
                });

        // Re-try creation
        bridgeExecutorService.createBridgeExecutor(dto);

        // Re-trying infers the status as FAILED and notifies the manager again.
        verify(managerClient, times(2)).notifyProcessorStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> assertProcessorManagedResourceStatusUpdateDTOUpdate(d,
                dto.getId(),
                dto.getCustomerId(),
                dto.getBridgeId(),
                FAILED));
    }

    @Test
    public void testBridgeExecutorCreationTimeout() {
        // Given a PROVISIONING Processor
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        dto.setStatus(PROVISIONING);

        // When - No dependencies are provisioned within the timeout. The deployment will fail.
        bridgeExecutorService.createBridgeExecutor(dto);

        ArgumentCaptor<ProcessorManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ProcessorManagedResourceStatusUpdateDTO.class);

        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // When the reconciliation completes the DTO remains in PROVISIONING, but we've notified the Manager that it is FAILED
                    assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
                    verify(managerClient).notifyProcessorStatusChange(updateDTO.capture());
                    updateDTO.getAllValues().forEach((d) -> assertProcessorManagedResourceStatusUpdateDTOUpdate(d,
                            dto.getId(),
                            dto.getCustomerId(),
                            dto.getBridgeId(),
                            FAILED));
                    verify(metricsService).onOperationFailed(any(BridgeExecutor.class), eq(MetricsOperation.CONTROLLER_RESOURCE_PROVISION));
                });

        // Re-try creation
        bridgeExecutorService.createBridgeExecutor(dto);

        // Since the DTO remained in PROVISIONING the manager is not notified again.
        verify(managerClient).notifyProcessorStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> assertProcessorManagedResourceStatusUpdateDTOUpdate(d,
                dto.getId(),
                dto.getCustomerId(),
                dto.getBridgeId(),
                FAILED));
    }

    @Test
    public void testBridgeExecutorRecreationTimeout() {
        // Given a PROVISIONING Processor
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        dto.setStatus(PROVISIONING);

        deployExecutorSuccessfully(dto);

        // Delete Deployment (mimicking a change in the environment). This re-triggers the reconcile loop.
        // However, we're not mocking its successful re-provision (nor other dependencies). This will lead
        // to a timeout of the BridgeExecutor re-provisioning.
        kubernetesClient.resources(Deployment.class).inAnyNamespace().delete();

        ArgumentCaptor<ProcessorManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ProcessorManagedResourceStatusUpdateDTO.class);

        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollDelay(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(managerClient, times(2)).notifyProcessorStatusChange(updateDTO.capture());
                    List<ProcessorManagedResourceStatusUpdateDTO> values = updateDTO.getAllValues();
                    // The first notification is for the original, successful, provisioning.
                    assertProcessorManagedResourceStatusUpdateDTOUpdate(values.get(0),
                            dto.getId(),
                            dto.getCustomerId(),
                            dto.getBridgeId(),
                            READY);
                    // The second notification is for the subsequent, unsuccessful, re-provisioning.
                    assertProcessorManagedResourceStatusUpdateDTOUpdate(values.get(1),
                            dto.getId(),
                            dto.getCustomerId(),
                            dto.getBridgeId(),
                            FAILED);
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

    private void waitUntilBridgeExecutorSecretAvailable(ProcessorDTO dto) {
        await(Duration.ofSeconds(5),
                Duration.ofMillis(100),
                () -> {
                    BridgeExecutor bridgeExecutor = fetchBridgeExecutor(dto);
                    assertThat(bridgeExecutor.getStatus().isConditionTypeTrue(BridgeExecutorStatus.SECRET_AVAILABLE)).isTrue();
                });
    }
}
