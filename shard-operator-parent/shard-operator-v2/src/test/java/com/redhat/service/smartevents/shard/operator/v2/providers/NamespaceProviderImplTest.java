package com.redhat.service.smartevents.shard.operator.v2.providers;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class NamespaceProviderImplTest {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NamespaceProviderImpl namespaceProvider;

    private ManagedBridge createManagedBridge() {

        String id = UUID.randomUUID().toString();
        String expectedNamespace = namespaceProvider.getNamespaceName(id);
        ManagedBridge mb = ManagedBridge.fromBuilder()
                .withBridgeId(id)
                .withNamespace(expectedNamespace)
                .withCustomerId(UUID.randomUUID().toString())
                .withBridgeName(UUID.randomUUID().toString())
                .withHost(UUID.randomUUID().toString())
                .withOwner(UUID.randomUUID().toString())
                .build();

        return mb;
    }

    @Test
    public void generateNamespaceName() {
        ManagedBridge mb = createManagedBridge();
        String namespaceName = namespaceProvider.getNamespaceName(mb.getSpec().getId());
        assertThat(namespaceName).isEqualTo(NamespaceProvider.NAMESPACE_NAME_PREFIX + mb.getSpec().getId());
    }

    @Test
    public void createNamespace() {

        ManagedBridge mb = createManagedBridge();

        String expectedNamespace = namespaceProvider.getNamespaceName(mb.getSpec().getId());
        assertThat(kubernetesClient.namespaces().withName(expectedNamespace).get()).isNull();

        Namespace namespace = namespaceProvider.fetchOrCreateNamespace(mb);
        assertThat(namespace.getMetadata().getName()).isEqualTo(expectedNamespace);

        namespace = kubernetesClient.namespaces().withName(expectedNamespace).get();
        assertThat(namespace).isNotNull();

        Map<String, String> labels = namespace.getMetadata().getLabels();
        assertThat(labels.get(LabelsBuilder.BRIDGE_ID_LABEL)).isEqualTo(mb.getSpec().getId());
        assertThat(labels.get(LabelsBuilder.BRIDGE_NAME_LABEL)).isEqualTo(mb.getSpec().getName());
        assertThat(labels.get(LabelsBuilder.CUSTOMER_ID_LABEL)).isEqualTo(mb.getSpec().getCustomerId());
    }

    @Test
    public void updateNamespaceLabels() {

        ManagedBridge mb = createManagedBridge();

        String expectedNamespace = namespaceProvider.getNamespaceName(mb.getSpec().getId());
        assertThat(kubernetesClient.namespaces().withName(expectedNamespace).get()).isNull();

        Namespace namespace = namespaceProvider.fetchOrCreateNamespace(mb);
        assertThat(namespace.getMetadata().getName()).isEqualTo(expectedNamespace);

        namespace = kubernetesClient.namespaces().withName(expectedNamespace).get();
        assertThat(namespace).isNotNull();

        Map<String, String> labels = namespace.getMetadata().getLabels();
        assertThat(labels.get(LabelsBuilder.BRIDGE_ID_LABEL)).isEqualTo(mb.getSpec().getId());
        assertThat(labels.get(LabelsBuilder.BRIDGE_NAME_LABEL)).isEqualTo(mb.getSpec().getName());
        assertThat(labels.get(LabelsBuilder.CUSTOMER_ID_LABEL)).isEqualTo(mb.getSpec().getCustomerId());

        String newName = "my-new-name";
        mb.getSpec().setName(newName);
        namespaceProvider.fetchOrCreateNamespace(mb);

        namespace = kubernetesClient.namespaces().withName(expectedNamespace).get();
        assertThat(namespace).isNotNull();

        labels = namespace.getMetadata().getLabels();
        assertThat(labels.get(LabelsBuilder.BRIDGE_ID_LABEL)).isEqualTo(mb.getSpec().getId());
        assertThat(labels.get(LabelsBuilder.BRIDGE_NAME_LABEL)).isEqualTo(mb.getSpec().getName());
        assertThat(labels.get(LabelsBuilder.CUSTOMER_ID_LABEL)).isEqualTo(mb.getSpec().getCustomerId());
    }

    @Test
    public void deleteNamespace() {

        ManagedBridge mb = createManagedBridge();
        String expectedName = namespaceProvider.getNamespaceName(mb.getSpec().getId());
        assertThat(kubernetesClient.namespaces().withName(expectedName).get()).isNull();

        namespaceProvider.fetchOrCreateNamespace(mb);

        Namespace namespace = kubernetesClient.namespaces().withName(expectedName).get();
        assertThat(namespace).isNotNull();
        mb.getMetadata().setNamespace(namespace.getMetadata().getName());

        namespaceProvider.deleteNamespace(mb);
        assertThat(kubernetesClient.namespaces().withName(expectedName).get()).isNull();
    }

    @Test
    public void deleteNamespace_namespaceDoesntExist() {

        ManagedBridge mb = createManagedBridge();
        String expectedName = namespaceProvider.getNamespaceName(mb.getSpec().getId());
        assertThat(kubernetesClient.namespaces().withName(expectedName).get()).isNull();

        namespaceProvider.deleteNamespace(mb);
    }
}
