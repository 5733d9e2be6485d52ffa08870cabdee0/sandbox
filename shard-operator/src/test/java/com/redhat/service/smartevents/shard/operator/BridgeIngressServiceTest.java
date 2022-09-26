package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.shard.operator.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.FAILED;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.shard.operator.utils.AwaitilityUtil.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class BridgeIngressServiceTest {

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

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

        when(templateProvider.loadBridgeIngressSecretTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeIngressConfigMapTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeIngressAuthorizationPolicyTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeIngressBrokerTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeIngressKubernetesIngressTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeIngressOpenshiftRouteTemplate(any(), any())).thenCallRealMethod();

        // Far from ideal... but each test assumes there are no other BridgeIngress instances in existence.
        // Unfortunately, however, some tests only check that provisioning either progressed to a certain
        // point of failed completely. There is therefore a good chance there's an incomplete BridgeIngress
        // in k8s when a subsequent test starts. This leads to non-deterministic behaviour of tests.
        // This ensures each test has a "clean" k8s environment.
        await(Duration.ofMinutes(1),
                Duration.ofSeconds(10),
                () -> assertThat(kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().list().getItems().isEmpty()).isTrue());
    }

    @Test
    public void testBridgeIngressCreation() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);
        waitUntilBridgeIngressExists(dto);

        // Then
        BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
        assertThat(bridgeIngress).isNotNull();

        Secret secret = fetchBridgeIngressSecret(dto);
        assertThat(secret).isNotNull();
        assertThat(secret.getMetadata().getName()).isEqualTo(bridgeIngress.getMetadata().getName());
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_PROTOCOL_SECRET).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_USER_SECRET).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_PASSWORD_SECRET).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_PROTOCOL_SECRET).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM_SECRET).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.TLS_CERTIFICATE_SECRET).length()).isGreaterThan(0);
        assertThat(secret.getData().get(GlobalConfigurationsConstants.TLS_KEY_SECRET).length()).isGreaterThan(0);
    }

    @Test
    public void testBridgeIngressCreationTriggersController() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);

        // Then
        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    Secret secret = fetchBridgeIngressSecret(dto);
                    assertThat(secret).isNotNull();

                    ConfigMap configMap = fetchBridgeIngressConfigMap(dto);
                    assertThat(configMap).isNotNull();
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_PARTITIONS_CONFIGMAP).length()).isGreaterThan(0);
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_REPLICATION_FACTOR_CONFIGMAP).length()).isGreaterThan(0);
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_BOOTSTRAP_SERVERS_CONFIGMAP).length()).isGreaterThan(0);
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_SECRET_REF_NAME_CONFIGMAP).length()).isGreaterThan(0);
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_TOPIC_NAME_CONFIGMAP).length()).isGreaterThan(0);

                    KnativeBroker knativeBroker = fetchBridgeIngressKnativeBroker(dto);
                    assertThat(knativeBroker).isNotNull();
                    assertThat(knativeBroker.getSpec().getConfig().getName().length()).isGreaterThan(0);
                    assertThat(knativeBroker.getSpec().getConfig().getKind().length()).isGreaterThan(0);
                    assertThat(knativeBroker.getSpec().getConfig().getNamespace().length()).isGreaterThan(0);
                    assertThat(knativeBroker.getSpec().getConfig().getApiVersion().length()).isGreaterThan(0);
                    kubernetesResourcePatcher.patchReadyKnativeBroker(knativeBroker.getMetadata().getName(), knativeBroker.getMetadata().getNamespace());

                    AuthorizationPolicy authorizationPolicy = fetchBridgeIngressAuthorizationPolicy(dto);
                    assertThat(authorizationPolicy).isNotNull();
                    assertThat(authorizationPolicy.getSpec().getAction().length()).isGreaterThan(0);
                    assertThat(authorizationPolicy.getSpec().getRules().get(0).getTo().size()).isGreaterThan(0);
                    assertThat(authorizationPolicy.getSpec().getRules().get(0).getTo().get(0).getOperation().getPaths().get(0).length()).isGreaterThan(0);
                    assertThat(authorizationPolicy.getSpec().getRules().get(0).getTo().get(0).getOperation().getMethods().get(0).length()).isGreaterThan(0);
                    assertThat(authorizationPolicy.getSpec().getRules().get(0).getWhen().size()).isGreaterThan(0);
                    assertThat(authorizationPolicy.getSpec().getRules().get(0).getWhen().get(0).getKey().length()).isGreaterThan(0);
                    assertThat(authorizationPolicy.getSpec().getRules().get(0).getWhen().get(0).getValues().size()).isGreaterThan(0);
                    assertThat(authorizationPolicy.getSpec().getRules().get(0).getWhen().get(0).getValues().get(0).length()).isGreaterThan(0);
                });
    }

    @Test
    public void testBridgeIngressDeletion() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);
        waitUntilBridgeIngressExists(dto);
        bridgeIngressService.deleteBridgeIngress(dto);
        waitUntilBridgeIngressDoesntExist(dto);

        // Then
        BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
        assertThat(bridgeIngress).isNull();
    }

    @Test
    public void testBridgeIngressCreationWhenSpecAlreadyExistsAsProvisioning() {
        // Given a PROVISIONING Bridge
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When
        bridgeIngressService.createBridgeIngress(dto);

        // Then
        // Manager is not notified
        assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
        verifyNoInteractions(managerClient);

        // Re-try creation
        bridgeIngressService.createBridgeIngress(dto);

        // Manager is still not notified as the BridgeIngress is not yet ready
        assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
        verifyNoInteractions(managerClient);
    }

    @Test
    public void testBridgeIngressCreationWhenSpecAlreadyExistsAsReady() {
        // Given a PROVISIONING Bridge
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        deployIngressSuccessfully(dto);

        // Re-try creation
        bridgeIngressService.createBridgeIngress(dto);

        ArgumentCaptor<ManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ManagedResourceStatusUpdateDTO.class);

        // (1) READY from original deployment, (2) READY from this subsequent re-deployment
        verify(managerClient, times(2)).notifyBridgeStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> assertManagedResourceStatusUpdateDTOUpdate(d, dto.getId(), dto.getCustomerId(), READY));
    }

    private void deployIngressSuccessfully(BridgeDTO dto) {
        // When
        bridgeIngressService.createBridgeIngress(dto);

        // Then
        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    KnativeBroker knativeBroker = fetchBridgeIngressKnativeBroker(dto);
                    kubernetesResourcePatcher.patchReadyKnativeBroker(knativeBroker.getMetadata().getName(), knativeBroker.getMetadata().getNamespace());
                });

        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
                    Service service = istioGatewayProvider.getIstioGatewayService();
                    kubernetesResourcePatcher.patchReadyNetworkResource(bridgeIngress.getMetadata().getName(), service.getMetadata().getNamespace());
                });

        await(Duration.ofMinutes(2),
                Duration.ofSeconds(5),
                () -> {
                    BridgeIngress fetched = fetchBridgeIngress(dto);
                    assertThat(fetched.getStatus().isReady()).isTrue();
                });

        ArgumentCaptor<ManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ManagedResourceStatusUpdateDTO.class);

        // When the reconciliation completes the DTO remains in PROVISIONING, but we've notified the Manager that it is READY
        assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
        verify(managerClient).notifyBridgeStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> assertManagedResourceStatusUpdateDTOUpdate(d, dto.getId(), dto.getCustomerId(), READY));
        verify(metricsService).onOperationComplete(any(BridgeIngress.class), eq(MetricsOperation.CONTROLLER_RESOURCE_PROVISION));
    }

    private void assertManagedResourceStatusUpdateDTOUpdate(ManagedResourceStatusUpdateDTO update,
            String expectedId,
            String expectedCustomerId,
            ManagedResourceStatus expectedStatus) {
        assertThat(update.getId()).isEqualTo(expectedId);
        assertThat(update.getCustomerId()).isEqualTo(expectedCustomerId);
        assertThat(update.getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void testBridgeIngressCreationWhenSpecAlreadyExistsAsFailed() {
        // Given a PROVISIONING Bridge
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // Mock an exception being thrown by the controller
        // k8s max-reties is set to 1 in application.properties, overriding the default
        // See https://github.com/quarkiverse/quarkus-operator-sdk/issues/380#issuecomment-1211343353
        reset(templateProvider);
        when(templateProvider.loadBridgeIngressSecretTemplate(any(), any())).thenCallRealMethod();
        when(templateProvider.loadBridgeIngressConfigMapTemplate(any(), any())).thenThrow(new InternalPlatformException("template-provider-error"));

        // When
        bridgeIngressService.createBridgeIngress(dto);

        ArgumentCaptor<ManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ManagedResourceStatusUpdateDTO.class);

        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // When the reconciliation completes the DTO remains in PROVISIONING, but we've notified the Manager that it is FAILED
                    assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
                    verify(managerClient, times(1)).notifyBridgeStatusChange(updateDTO.capture());
                    updateDTO.getAllValues().forEach((d) -> assertManagedResourceStatusUpdateDTOUpdate(d, dto.getId(), dto.getCustomerId(), FAILED));
                    verify(metricsService).onOperationFailed(any(BridgeIngress.class), eq(MetricsOperation.CONTROLLER_RESOURCE_PROVISION));
                });

        // Re-try creation
        bridgeIngressService.createBridgeIngress(dto);

        verify(managerClient, times(2)).notifyBridgeStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> assertManagedResourceStatusUpdateDTOUpdate(d, dto.getId(), dto.getCustomerId(), FAILED));
    }

    @Test
    public void testBridgeIngressCreationTimeout() {
        // Given a PROVISIONING Bridge
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        // When - KnativeBroker is not provisioned within the timeout. The deployment will fail.
        bridgeIngressService.createBridgeIngress(dto);

        ArgumentCaptor<ManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ManagedResourceStatusUpdateDTO.class);

        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollDelay(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    // When the reconciliation completes the DTO remains in PROVISIONING, but we've notified the Manager that it is FAILED
                    assertThat(dto.getStatus()).isEqualTo(PROVISIONING);
                    verify(managerClient, times(1)).notifyBridgeStatusChange(updateDTO.capture());
                    updateDTO.getAllValues().forEach((d) -> assertManagedResourceStatusUpdateDTOUpdate(d, dto.getId(), dto.getCustomerId(), FAILED));
                    verify(metricsService).onOperationFailed(any(BridgeIngress.class), eq(MetricsOperation.CONTROLLER_RESOURCE_PROVISION));
                });
    }

    @Test
    public void testBridgeIngressRecreationTimeout() {
        // Given a PROVISIONING Bridge
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();

        deployIngressSuccessfully(dto);

        // Delete KNative Broker (mimicking a change in the environment). This re-triggers the reconcile loop.
        // However, we're not mocking its successful re-provision (nor other dependencies). This will lead
        // to a timeout of the BridgeIngress re-provisioning.
        kubernetesClient.resources(KnativeBroker.class).inAnyNamespace().delete();

        ArgumentCaptor<ManagedResourceStatusUpdateDTO> updateDTO = ArgumentCaptor.forClass(ManagedResourceStatusUpdateDTO.class);

        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollDelay(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(managerClient, times(2)).notifyBridgeStatusChange(updateDTO.capture());
                    List<ManagedResourceStatusUpdateDTO> values = updateDTO.getAllValues();
                    // The first notification is for the original, successful, provisioning.
                    assertManagedResourceStatusUpdateDTOUpdate(values.get(0), dto.getId(), dto.getCustomerId(), READY);
                    // The second notification is for the subsequent, unsuccessful, re-provisioning.
                    assertManagedResourceStatusUpdateDTOUpdate(values.get(1), dto.getId(), dto.getCustomerId(), FAILED);
                });
    }

    private BridgeIngress fetchBridgeIngress(BridgeDTO dto) {
        return kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private Secret fetchBridgeIngressSecret(BridgeDTO dto) {
        return kubernetesClient
                .secrets()
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private ConfigMap fetchBridgeIngressConfigMap(BridgeDTO dto) {
        return kubernetesClient
                .configMaps()
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private AuthorizationPolicy fetchBridgeIngressAuthorizationPolicy(BridgeDTO dto) {
        return kubernetesClient
                .resources(AuthorizationPolicy.class)
                .inNamespace(istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace())
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private KnativeBroker fetchBridgeIngressKnativeBroker(BridgeDTO dto) {
        return kubernetesClient
                .resources(KnativeBroker.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private void waitUntilBridgeIngressExists(BridgeDTO dto) {
        await(Duration.ofSeconds(30),
                Duration.ofMillis(200),
                () -> {
                    BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
                    assertThat(bridgeIngress).isNotNull();
                });
    }

    private void waitUntilBridgeIngressDoesntExist(BridgeDTO dto) {
        await(Duration.ofSeconds(30),
                Duration.ofMillis(200),
                () -> {
                    BridgeIngress bridgeIngress = fetchBridgeIngress(dto);
                    assertThat(bridgeIngress).isNull();
                });
    }
}
