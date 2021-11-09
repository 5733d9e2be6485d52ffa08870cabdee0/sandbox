package com.redhat.service.bridge.shard.operator.providers;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.shard.operator.BridgeIngressService;
import com.redhat.service.bridge.shard.operator.TestConstants;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithKubernetesTestServer
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
        final BridgeDTO dto = TestConstants.newRequestedBridgeDTO();
        dto.setCustomerId("cooper");
        bridgeIngressService.createBridgeIngress(dto);
        // try to delete the namespace...
        customerNamespaceProvider.deleteCustomerNamespaceIfEmpty(dto.getCustomerId());
        final Namespace namespace = kubernetesClient.namespaces().withName(customerNamespaceProvider.resolveName(dto.getCustomerId())).get();
        assertThat(namespace).isNotNull();
    }

    @Test
    void testNamespaceDeletedWhenEmpty() {
        final BridgeDTO dto = TestConstants.newRequestedBridgeDTO();
        dto.setCustomerId("hofstadter");
        bridgeIngressService.createBridgeIngress(dto);
        // there's only one bridge there
        bridgeIngressService.deleteBridgeIngress(dto);
        final Namespace namespace = kubernetesClient.namespaces().withName(customerNamespaceProvider.resolveName(dto.getCustomerId())).get();
        assertThat(namespace).isNull();
    }
}
