package com.redhat.service.smartevents.shard.operator.v2.providers;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v2.api.models.Operation;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v2.Fixtures;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class NamespaceProviderImplTest {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NamespaceProviderImpl namespaceProvider;

    private ManagedBridge createManagedBridge() {
        BridgeDTO bridgeDTO = Fixtures.createBridge(Operation.CREATE);
        String expectedNamespace = namespaceProvider.getNamespaceName(bridgeDTO.getId());
        return Fixtures.createManagedBridge(bridgeDTO, expectedNamespace);
    }

    @BeforeEach
    public void before() {
        kubernetesClient.namespaces().delete();
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
        assertThat(labels).containsEntry(LabelsBuilder.BRIDGE_ID_LABEL, mb.getSpec().getId());
        assertThat(labels).containsEntry(LabelsBuilder.BRIDGE_NAME_LABEL, mb.getSpec().getName());
        assertThat(labels).containsEntry(LabelsBuilder.CUSTOMER_ID_LABEL, mb.getSpec().getCustomerId());
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
        assertThat(labels).containsEntry(LabelsBuilder.BRIDGE_ID_LABEL, mb.getSpec().getId());
        assertThat(labels).containsEntry(LabelsBuilder.BRIDGE_NAME_LABEL, mb.getSpec().getName());
        assertThat(labels).containsEntry(LabelsBuilder.CUSTOMER_ID_LABEL, mb.getSpec().getCustomerId());

        String newName = "my-new-name";
        mb.getSpec().setName(newName);
        namespaceProvider.fetchOrCreateNamespace(mb);

        namespace = kubernetesClient.namespaces().withName(expectedNamespace).get();
        assertThat(namespace).isNotNull();

        labels = namespace.getMetadata().getLabels();
        assertThat(labels).containsEntry(LabelsBuilder.BRIDGE_ID_LABEL, mb.getSpec().getId());
        assertThat(labels).containsEntry(LabelsBuilder.BRIDGE_NAME_LABEL, mb.getSpec().getName());
        assertThat(labels).containsEntry(LabelsBuilder.CUSTOMER_ID_LABEL, mb.getSpec().getCustomerId());

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
