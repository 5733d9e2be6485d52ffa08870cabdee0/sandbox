package com.redhat.service.smartevents.shard.operator.v2;

import java.time.Duration;
import java.util.Base64;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.core.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.DNSConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;
import com.redhat.service.smartevents.shard.operator.v2.utils.V2KubernetesResourcePatcher;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagedBridgeServiceImplTest {

    @Inject
    ManagedBridgeService managedBridgeService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NamespaceProvider namespaceProvider;

    @Inject
    V2KubernetesResourcePatcher kubernetesResourcePatcher;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    Base64.Encoder encoder = Base64.getEncoder();

    @BeforeEach
    public void beforeEach() {
        kubernetesResourcePatcher.cleanUp();
    }

    @Test
    public void createManagedBridge() {
        BridgeDTO bridgeDTO = Fixtures.createBridge(OperationType.CREATE);
        managedBridgeService.createManagedBridge(bridgeDTO);

        waitUntilManagedBridgeExists(bridgeDTO);

        ManagedBridge managedBridge = fetchManagedBridge(bridgeDTO);

        Secret secret = fetchManagedBridgeSecret(bridgeDTO);
        assertThat(secret).isNotNull();
        assertThat(secret.getMetadata().getName()).isEqualTo(managedBridge.getMetadata().getName());

        KafkaConfigurationSpec kafkaConfiguration = managedBridge.getSpec().getkNativeBrokerConfiguration().getKafkaConfiguration();

        assertThat(secret.getData()).containsEntry(GlobalConfigurationsConstants.KNATIVE_KAFKA_PROTOCOL_SECRET, encode(kafkaConfiguration.getSecurityProtocol()));
        assertThat(secret.getData()).containsEntry(GlobalConfigurationsConstants.KNATIVE_KAFKA_USER_SECRET, encode(kafkaConfiguration.getUser()));
        assertThat(secret.getData()).containsEntry(GlobalConfigurationsConstants.KNATIVE_KAFKA_PASSWORD_SECRET, encode(kafkaConfiguration.getPassword()));
        assertThat(secret.getData()).containsEntry(GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM_SECRET, encode(kafkaConfiguration.getSaslMechanism()));
        assertThat(secret.getData()).containsEntry(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET, encode(kafkaConfiguration.getTopic()));
        assertThat(secret.getData()).containsEntry(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET, encode(kafkaConfiguration.getBootstrapServers()));

        DNSConfigurationSpec dnsConfiguration = managedBridge.getSpec().getDnsConfiguration();

        assertThat(secret.getData()).containsEntry(GlobalConfigurationsConstants.TLS_CERTIFICATE_SECRET, encode(dnsConfiguration.getTls().getCertificate()));
        assertThat(secret.getData()).containsEntry(GlobalConfigurationsConstants.TLS_KEY_SECRET, encode(dnsConfiguration.getTls().getKey()));
    }

    private String encode(String value) {
        return encoder.encodeToString(value.getBytes());
    }

    @Test
    public void managedBridgeCreationTriggersController() {
        // Given
        BridgeDTO bridgeDTO = Fixtures.createBridge(OperationType.CREATE);

        // When
        managedBridgeService.createManagedBridge(bridgeDTO);

        // Then
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Secret secret = fetchBridgeSecret(bridgeDTO);
                    assertThat(secret).isNotNull();

                    ConfigMap configMap = fetchBridgeConfigMap(bridgeDTO);
                    assertThat(configMap).isNotNull();
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_PARTITIONS_CONFIGMAP).length()).isGreaterThan(0);
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_REPLICATION_FACTOR_CONFIGMAP).length()).isGreaterThan(0);
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_BOOTSTRAP_SERVERS_CONFIGMAP).length()).isGreaterThan(0);
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_SECRET_REF_NAME_CONFIGMAP).length()).isGreaterThan(0);
                    assertThat(configMap.getData().get(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_TOPIC_NAME_CONFIGMAP).length()).isGreaterThan(0);

                    KnativeBroker knativeBroker = fetchBridgeKnativeBroker(bridgeDTO);
                    assertThat(knativeBroker).isNotNull();
                    assertThat(knativeBroker.getSpec().getConfig().getName().length()).isGreaterThan(0);
                    assertThat(knativeBroker.getSpec().getConfig().getKind().length()).isGreaterThan(0);
                    assertThat(knativeBroker.getSpec().getConfig().getNamespace().length()).isGreaterThan(0);
                    assertThat(knativeBroker.getSpec().getConfig().getApiVersion().length()).isGreaterThan(0);
                    kubernetesResourcePatcher.patchReadyKnativeBroker(knativeBroker.getMetadata().getName(), knativeBroker.getMetadata().getNamespace());

                    AuthorizationPolicy authorizationPolicy = fetchBridgeIngressAuthorizationPolicy(bridgeDTO);
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

    private <T extends HasMetadata> T assertV2Labels(T hasMetaData) {
        assertThat(hasMetaData).isNotNull();
        assertThat(hasMetaData.getMetadata().getLabels()).containsEntry(LabelsBuilder.CREATED_BY_LABEL, LabelsBuilder.V2_OPERATOR_NAME);
        assertThat(hasMetaData.getMetadata().getLabels()).containsEntry(LabelsBuilder.MANAGED_BY_LABEL, LabelsBuilder.V2_OPERATOR_NAME);
        return hasMetaData;
    }

    private AuthorizationPolicy fetchBridgeIngressAuthorizationPolicy(BridgeDTO dto) {
        return assertV2Labels(kubernetesClient
                .resources(AuthorizationPolicy.class)
                .inNamespace(istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace())
                .withName(dto.getId())
                .get());
    }

    private Secret fetchBridgeSecret(BridgeDTO dto) {
        return assertV2Labels(kubernetesClient
                .secrets()
                .inNamespace(namespaceProvider.getNamespaceName(dto.getId()))
                .withName(dto.getId())
                .get());
    }

    private void waitUntilManagedBridgeExists(BridgeDTO dto) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    ManagedBridge mb = fetchManagedBridge(dto);
                    assertThat(mb).isNotNull();
                });
    }

    private ConfigMap fetchBridgeConfigMap(BridgeDTO dto) {
        return assertV2Labels(kubernetesClient
                .configMaps()
                .inNamespace(namespaceProvider.getNamespaceName(dto.getId()))
                .withName(dto.getId())
                .get());
    }

    private KnativeBroker fetchBridgeKnativeBroker(BridgeDTO dto) {
        return assertV2Labels(kubernetesClient
                .resources(KnativeBroker.class)
                .inNamespace(namespaceProvider.getNamespaceName(dto.getId()))
                .withName(dto.getId())
                .get());
    }

    private Secret fetchManagedBridgeSecret(BridgeDTO dto) {
        return assertV2Labels(kubernetesClient
                .resources(Secret.class)
                .inNamespace(namespaceProvider.getNamespaceName(dto.getId()))
                .withName(dto.getId())
                .get());
    }

    private ManagedBridge fetchManagedBridge(BridgeDTO dto) {
        return assertV2Labels(kubernetesClient
                .resources(ManagedBridge.class)
                .inNamespace(namespaceProvider.getNamespaceName(dto.getId()))
                .withName(dto.getId())
                .get());
    }
}
