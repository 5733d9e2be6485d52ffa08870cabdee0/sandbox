package com.redhat.service.smartevents.shard.operator.v2.providers;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class NamespaceProviderImpl implements NamespaceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceProviderImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public Namespace fetchOrCreateNamespace(ManagedBridge managedBridge) {
        final String name = getNamespaceName(managedBridge.getSpec().getId());
        final Namespace namespace = kubernetesClient.namespaces().withName(name).get();
        if (namespace == null) {
            LOGGER.info("Creating Namespace '{}' for ManagedBridge with id '{}'", name, managedBridge.getSpec().getId());
            return kubernetesClient.namespaces().createOrReplace(
                    new NamespaceBuilder()
                            .withNewMetadata()
                            .withName(name)
                            .withLabels(this.createLabels(managedBridge))
                            .endMetadata()
                            .build());
        }
        return ensureManagedLabels(namespace, managedBridge);
    }

    private Namespace ensureManagedLabels(final Namespace namespace, final ManagedBridge managedBridge) {
        final Map<String, String> labels = this.createLabels(managedBridge);
        boolean mustUpdate = (namespace.getMetadata().getLabels() == null || namespace.getMetadata().getLabels().isEmpty());
        if (!mustUpdate) {
            for (Map.Entry<String, String> label : labels.entrySet()) {
                if (!label.getValue().equals(namespace.getMetadata().getLabels().get(label.getKey()))) {
                    mustUpdate = true;
                    break;
                }
            }
        }
        if (mustUpdate) {
            LOGGER.info("Updating labels on existing Namespace '{}' for ManagedBridge with id '{}'", namespace.getMetadata().getName(), managedBridge.getSpec().getId());
            return kubernetesClient.namespaces().withName(
                    namespace.getMetadata().getName()).edit(n -> new NamespaceBuilder(n).editOrNewMetadata().addToLabels(labels).endMetadata().build());
        }
        return namespace;
    }

    private Map<String, String> createLabels(final ManagedBridge managedBridge) {
        return new LabelsBuilder()
                .withCustomerId(managedBridge.getSpec().getCustomerId())
                .withBridgeId(managedBridge.getSpec().getId())
                .withBridgeName(managedBridge.getSpec().getName())
                .buildWithDefaults();
    }

    @Override
    public void deleteNamespace(ManagedBridge managedBridge) {
        String namespaceName = managedBridge.getMetadata().getNamespace();
        Namespace namespace = kubernetesClient.namespaces().withName(namespaceName).get();
        if (namespace != null) {
            kubernetesClient.namespaces().delete(namespace);
            LOGGER.info("Marked Namespace '{}' for ManagedBridge with id '{}' for deletion.", namespaceName, managedBridge.getSpec().getId());
        }
    }
}
