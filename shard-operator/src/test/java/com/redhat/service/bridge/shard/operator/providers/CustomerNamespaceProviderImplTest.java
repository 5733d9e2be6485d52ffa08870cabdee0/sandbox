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
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@WithKubernetesTestServer
class CustomerNamespaceProviderImplTest {

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    BridgeIngressService bridgeIngressService;

    @KubernetesTestServer
    KubernetesServer kubernetesTestServer;

    @Test
    void testNamespaceIsCreated() {
        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace("123");
        assertNotNull(namespace);
        assertEquals(customerNamespaceProvider.resolveName("123"), namespace.getMetadata().getName());
    }

    @Test
    void testRawNamespaceIsFetchedEnsureLabels() {
        final String name = customerNamespaceProvider.resolveName("xyz");
        kubernetesTestServer.getClient().namespaces().create(new NamespaceBuilder().withNewMetadata().withName(name).and().build());

        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace("xyz");
        assertNotNull(namespace);
        assertEquals(name, namespace.getMetadata().getName());
        assertNotNull(namespace.getMetadata().getLabels());
        assertFalse(namespace.getMetadata().getLabels().isEmpty());
        assertTrue(namespace.getMetadata().getLabels().containsKey(LabelsBuilder.MANAGED_BY_LABEL));
        assertTrue(namespace.getMetadata().getLabels().containsKey(LabelsBuilder.CREATED_BY_LABEL));
    }

    @Test
    void testNamespaceIsFetchedEnsureOwnLabels() {
        final String name = customerNamespaceProvider.resolveName("zyx");
        kubernetesTestServer.getClient().namespaces().create(
                new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(name)
                        .addToLabels(Collections.singletonMap("app", "test"))
                        .and().build());

        final Namespace namespace = customerNamespaceProvider.fetchOrCreateCustomerNamespace("zyx");
        assertNotNull(namespace);
        assertEquals(name, namespace.getMetadata().getName());
        assertNotNull(namespace.getMetadata().getLabels());
        assertFalse(namespace.getMetadata().getLabels().isEmpty());
        assertTrue(namespace.getMetadata().getLabels().containsKey(LabelsBuilder.MANAGED_BY_LABEL));
        assertTrue(namespace.getMetadata().getLabels().containsKey(LabelsBuilder.CREATED_BY_LABEL));
        assertTrue(namespace.getMetadata().getLabels().containsKey("app"));
    }

    @Test
    void testNamespaceNotDeletedWithBridges() {
        final BridgeDTO dto = TestConstants.newRequestedBridgeDTO();
        dto.setCustomerId("cooper");
        bridgeIngressService.createBridgeIngress(dto);
        // try to delete the namespace...
        customerNamespaceProvider.deleteCustomerNamespaceIfEmpty(dto.getCustomerId());
        final Namespace namespace = kubernetesTestServer.getClient().namespaces().withName(customerNamespaceProvider.resolveName(dto.getCustomerId())).get();
        assertNotNull(namespace);
    }

    @Test
    void testNamespaceDeletedWhenEmpty() {
        final BridgeDTO dto = TestConstants.newRequestedBridgeDTO();
        dto.setCustomerId("hofstadter");
        bridgeIngressService.createBridgeIngress(dto);
        // there's only one bridge there
        bridgeIngressService.deleteBridgeIngress(dto);
        final Namespace namespace = kubernetesTestServer.getClient().namespaces().withName(customerNamespaceProvider.resolveName(dto.getCustomerId())).get();
        assertNull(namespace);
    }
}
