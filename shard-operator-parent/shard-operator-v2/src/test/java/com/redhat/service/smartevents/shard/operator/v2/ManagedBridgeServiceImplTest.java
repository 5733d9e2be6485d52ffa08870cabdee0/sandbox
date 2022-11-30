package com.redhat.service.smartevents.shard.operator.v2;

import java.time.Duration;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagedBridgeServiceImplTest {

    @Inject
    ManagedBridgeService managedBridgeService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NamespaceProvider namespaceProvider;

    @Test
    public void createManagedBridge() {
        BridgeDTO bridgeDTO = Fixtures.createBridge(OperationType.CREATE);
        managedBridgeService.createManagedBridge(bridgeDTO);

        waitUntilManagedBridgeExists(bridgeDTO);

        ManagedBridge managedBridge = fetchManagedBridge(bridgeDTO);

        Secret secret = fetchManagedBridgeSecret(bridgeDTO);
        assertThat(secret).isNotNull();
        assertThat(secret.getMetadata().getName()).isEqualTo(managedBridge.getMetadata().getName());
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

    private void waitUntilManagedBridgeExists(BridgeDTO dto) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    ManagedBridge mb = fetchManagedBridge(dto);
                    assertThat(mb).isNotNull();
                });
    }

    private Secret fetchManagedBridgeSecret(BridgeDTO dto) {
        return kubernetesClient
                .resources(Secret.class)
                .inNamespace(namespaceProvider.getNamespaceName(dto.getId()))
                .withName(dto.getId())
                .get();
    }

    private ManagedBridge fetchManagedBridge(BridgeDTO dto) {
        return kubernetesClient
                .resources(ManagedBridge.class)
                .inNamespace(namespaceProvider.getNamespaceName(dto.getId()))
                .withName(dto.getId())
                .get();
    }
}
