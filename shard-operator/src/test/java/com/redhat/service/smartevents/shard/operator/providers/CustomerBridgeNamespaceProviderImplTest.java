package com.redhat.service.smartevents.shard.operator.providers;

import java.time.Duration;
import java.util.Collections;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.BridgeIngressService;
import com.redhat.service.smartevents.shard.operator.TestSupport;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

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
class CustomerBridgeNamespaceProviderImplTest {

    @Inject
    CustomerBridgeNamespaceProvider customerBridgeNamespaceProvider;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    void setup() {
        kubernetesResourcePatcher.cleanUp();
    }

    @Test
    void testNamespaceIsCreated() {
        final Namespace namespace = customerBridgeNamespaceProvider.fetchOrCreateCustomerBridgeNamespace("123", "456");
        assertThat(namespace).isNotNull();
        assertThat(customerBridgeNamespaceProvider.resolveName("123", "456")).isEqualTo(namespace.getMetadata().getName());
    }

    @Test
    void testRawNamespaceIsFetchedEnsureLabels() {
        final String name = customerBridgeNamespaceProvider.resolveName("zyx", "abc");
        kubernetesClient.namespaces().create(new NamespaceBuilder().withNewMetadata().withName(name).and().build());

        final Namespace namespace = customerBridgeNamespaceProvider.fetchOrCreateCustomerBridgeNamespace("zyx", "abc");
        assertThat(namespace).isNotNull();
        assertThat(name).isEqualTo(namespace.getMetadata().getName());
        assertThat(namespace.getMetadata().getLabels()).isNotNull();
        assertThat(namespace.getMetadata().getLabels()).isNotEmpty();
        assertThat(namespace.getMetadata().getLabels()).containsKey(LabelsBuilder.MANAGED_BY_LABEL);
        assertThat(namespace.getMetadata().getLabels()).containsKey(LabelsBuilder.CREATED_BY_LABEL);
    }

    @Test
    void testNamespaceIsFetchedEnsureOwnLabels() {
        final String name = customerBridgeNamespaceProvider.resolveName("xyz", "abc");
        kubernetesClient.namespaces().create(
                new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(name)
                        .addToLabels(Collections.singletonMap("app", "test"))
                        .and().build());

        final Namespace namespace = customerBridgeNamespaceProvider.fetchOrCreateCustomerBridgeNamespace("xyz", "abc");
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
                        .inNamespace(customerBridgeNamespaceProvider.resolveName(dto.getCustomerId(), dto.getId()))
                        .withName(BridgeIngress.resolveResourceName(dto.getId()))
                        .get() != null);
        // try to delete the namespace...
        customerBridgeNamespaceProvider.deleteNamespaceIfEmpty(kubernetesClient.namespaces().withName("ob-cooper-" + dto.getId()).get());
        final Namespace namespace = kubernetesClient.namespaces().withName(customerBridgeNamespaceProvider.resolveName(dto.getCustomerId(), dto.getId())).get();
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
                        .inNamespace(customerBridgeNamespaceProvider.resolveName(dto.getCustomerId(), dto.getId()))
                        .withName(BridgeIngress.resolveResourceName(dto.getId()))
                        .get() != null);
        // there's only one bridge there
        bridgeIngressService.deleteBridgeIngress(dto);
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .until(() -> kubernetesClient.resources(BridgeIngress.class).withName(BridgeIngress.resolveResourceName(dto.getId())).get() == null);

        customerBridgeNamespaceProvider.cleanUpEmptyNamespaces();
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .until(() -> kubernetesClient.namespaces().withName(customerBridgeNamespaceProvider.resolveName(dto.getCustomerId(), dto.getId())).get() == null);
    }
}
