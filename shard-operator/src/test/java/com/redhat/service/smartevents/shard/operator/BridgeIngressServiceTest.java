package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.shard.operator.providers.CustomerBridgeNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.shard.operator.utils.AwaitilityUtil.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class BridgeIngressServiceTest {

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerBridgeNamespaceProvider customerBridgeNamespaceProvider;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @InjectMock
    ManagerClient managerClient;

    @BeforeEach
    public void setup() {
        // Kubernetes Server must be cleaned up at startup of every test.
        kubernetesResourcePatcher.cleanUp();
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

        BridgeIngress bridgeIngressExisting = fetchBridgeIngress(dto);
        assertThat(bridgeIngressExisting).isNotNull();

        String nameSpaceName = bridgeIngressExisting.getMetadata().getNamespace();
        Namespace namespaceExisting = fetchNamespace(nameSpaceName);
        assertThat(namespaceExisting).isNotNull();

        bridgeIngressService.deleteBridgeIngress(dto);

        // Then
        waitUntilBridgeIngressDoesntExist(dto);
        Namespace namespaceDeleted = fetchNamespace(nameSpaceName);
        assertThat(namespaceDeleted).isNull();
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
        verify(managerClient, times(1)).notifyBridgeStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> {
            assertThat(d.getId()).isEqualTo(dto.getId());
            assertThat(d.getCustomerId()).isEqualTo(dto.getCustomerId());
            assertThat(d.getStatus()).isEqualTo(ManagedResourceStatus.READY);
        });

        // Re-try creation
        bridgeIngressService.createBridgeIngress(dto);

        verify(managerClient, times(2)).notifyBridgeStatusChange(updateDTO.capture());
        updateDTO.getAllValues().forEach((d) -> {
            assertThat(d.getId()).isEqualTo(dto.getId());
            assertThat(d.getCustomerId()).isEqualTo(dto.getCustomerId());
            assertThat(d.getStatus()).isEqualTo(ManagedResourceStatus.READY);
        });
    }

    private BridgeIngress fetchBridgeIngress(BridgeDTO dto) {
        return kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(customerBridgeNamespaceProvider.resolveName(dto.getCustomerId(), dto.getId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private Secret fetchBridgeIngressSecret(BridgeDTO dto) {
        return kubernetesClient
                .secrets()
                .inNamespace(customerBridgeNamespaceProvider.resolveName(dto.getCustomerId(), dto.getId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private ConfigMap fetchBridgeIngressConfigMap(BridgeDTO dto) {
        return kubernetesClient
                .configMaps()
                .inNamespace(customerBridgeNamespaceProvider.resolveName(dto.getCustomerId(), dto.getId()))
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
                .inNamespace(customerBridgeNamespaceProvider.resolveName(dto.getCustomerId(), dto.getId()))
                .withName(BridgeIngress.resolveResourceName(dto.getId()))
                .get();
    }

    private Namespace fetchNamespace(String namespace) {
        return kubernetesClient
                .namespaces()
                .withName(namespace)
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
