package com.redhat.service.bridge.shard.operator.providers;

import java.time.Duration;
import java.util.Collections;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.BridgeIngressService;
import com.redhat.service.bridge.shard.operator.TestSupport;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;
import com.redhat.service.bridge.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
class CustomerNamespaceProviderImplTest {

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    KubernetesClient kubernetesClient;

    @Test
    void testNamespaceIsCreated() {
        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace("123");
        assertThat(namespace).isNotNull();
        assertThat(customerNamespaceProvider.resolveName("123")).isEqualTo(namespace.getMetadata().getName());
    }

    @Test
    void testRawNamespaceIsFetchedEnsureLabels() {
        final String name = customerNamespaceProvider.resolveName("xyz");
        kubernetesClient.namespaces().create(new NamespaceBuilder().withNewMetadata().withName(name).and().build());

        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace("xyz");
        assertThat(namespace).isNotNull();
        assertThat(name).isEqualTo(namespace.getMetadata().getName());
        assertThat(namespace.getMetadata().getLabels()).isNotNull();
        assertThat(namespace.getMetadata().getLabels()).isNotEmpty();
        assertThat(namespace.getMetadata().getLabels()).containsKey(LabelsBuilder.MANAGED_BY_LABEL);
        assertThat(namespace.getMetadata().getLabels()).containsKey(LabelsBuilder.CREATED_BY_LABEL);
    }

    @Test
    void testNamespaceIsFetchedEnsureOwnLabels() {
        final String name = customerNamespaceProvider.resolveName("zyx");
        kubernetesClient.namespaces().create(
                new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(name)
                        .addToLabels(Collections.singletonMap("app", "test"))
                        .and().build());

        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace("zyx");
        assertThat(namespace).isNotNull();
        assertThat(name).isEqualTo(namespace.getMetadata().getName());
        assertThat(namespace.getMetadata().getLabels()).isNotNull();
        assertThat(namespace.getMetadata().getLabels()).isNotEmpty();
        assertThat(namespace.getMetadata().getLabels()).containsKey(LabelsBuilder.MANAGED_BY_LABEL);
        assertThat(namespace.getMetadata().getLabels()).containsKey(LabelsBuilder.CREATED_BY_LABEL);
        assertThat(namespace.getMetadata().getLabels()).containsKey("app");
    }

    @Test
    void testNamespaceNotDeletedWithBridges() {
        final BridgeDTO dto = TestSupport.newRequestedBridgeDTO();
        dto.setCustomerId("cooper");
        bridgeIngressService.createBridgeIngress(dto);
        // Wait until BridgeIngress is really created, can take some time due to client caching
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> kubernetesClient
                        .resources(BridgeIngress.class)
                        .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                        .withName(BridgeIngress.resolveResourceName(dto.getId()))
                        .get() != null);
        // try to delete the namespace...
        customerNamespaceProvider.deleteNamespaceIfEmpty(kubernetesClient.namespaces().withName("ob-cooper").get());
        final Namespace namespace = kubernetesClient.namespaces().withName(customerNamespaceProvider.resolveName(dto.getCustomerId())).get();
        assertThat(namespace).isNotNull();
    }

    @Test
    void testNamespaceDeletedWhenEmpty() {
        final BridgeDTO dto = TestSupport.newRequestedBridgeDTO();
        dto.setCustomerId("hofstadter");
        bridgeIngressService.createBridgeIngress(dto);
        // Wait until BridgeIngress is really created, can take some time due to client caching - https://issues.redhat.com/browse/MGDOBR-308
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> kubernetesClient
                        .resources(BridgeIngress.class)
                        .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                        .withName(BridgeIngress.resolveResourceName(dto.getId()))
                        .get() != null);
        // there's only one bridge there
        bridgeIngressService.deleteBridgeIngress(dto);
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> kubernetesClient.namespaces().withName(customerNamespaceProvider.resolveName(dto.getCustomerId())).get() == null);
    }
}
