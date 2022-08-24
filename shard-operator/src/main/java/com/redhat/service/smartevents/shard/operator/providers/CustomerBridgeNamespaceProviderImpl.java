package com.redhat.service.smartevents.shard.operator.providers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class CustomerBridgeNamespaceProviderImpl implements CustomerBridgeNamespaceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerBridgeNamespaceProviderImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public Namespace fetchOrCreateCustomerBridgeNamespace(String customerId, String bridgeId) {
        final String name = this.resolveName(customerId, bridgeId);
        final Namespace namespace = kubernetesClient.namespaces().withName(name).get();
        if (namespace == null) {
            return kubernetesClient.namespaces().createOrReplace(
                    new NamespaceBuilder()
                            .withNewMetadata()
                            .withName(name)
                            .withLabels(this.createLabels(customerId))
                            .endMetadata()
                            .build());
        }
        return ensureManagedLabels(namespace, customerId);
    }

    // Scheduled at midnight
    @Scheduled(cron = "0 0 0 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void cleanUpEmptyNamespaces() {
        List<Namespace> namespaces = kubernetesClient
                .namespaces()
                .withLabels(new LabelsBuilder().buildWithDefaults())
                .list()
                .getItems();
        for (Namespace namespace : namespaces) {
            deleteNamespaceIfEmpty(namespace);
        }
    }

    @Override
    public void deleteNamespaceIfEmpty(Namespace namespace) {
        if (isSafeToDelete(namespace)) {
            // deletion can be tricky with finalizers, pending objects, etc.
            // let's start simple and build on top of the constraints and scenarios as we evolve.
            if (!kubernetesClient.namespaces().withName(namespace.getMetadata().getName()).delete()) {
                LOGGER.warn("Namespace '{}' has not been deleted.", namespace.getMetadata().getName());
            }
        } else {
            LOGGER.warn("Namespace '{}' has not been deleted as it contained other resources.", namespace.getMetadata().getName());
            existingResources(BridgeIngress.class, namespace).forEach(r -> LOGGER.debug("Namespace '{}' contains: {}", namespace.getMetadata().getName(), r));
        }
    }

    private Map<String, String> createLabels(final String customerId) {
        return new LabelsBuilder().withCustomerId(customerId).buildWithDefaults();
    }

    private Namespace ensureManagedLabels(final Namespace namespace, final String customerId) {
        final Map<String, String> labels = this.createLabels(customerId);
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
            return kubernetesClient.namespaces().withName(
                    namespace.getMetadata().getName()).edit(n -> new NamespaceBuilder(n).editOrNewMetadata().addToLabels(labels).endMetadata().build());
        }
        return namespace;
    }

    private boolean isSafeToDelete(final Namespace namespace) {
        // as we evolve, chain the verification, add more constraints, etc.
        return isEmptyOf(BridgeIngress.class, namespace);
    }

    private <T extends HasMetadata> boolean isEmptyOf(Class<T> managedShardResource, Namespace namespace) {
        return existingResources(managedShardResource, namespace).isEmpty();
    }

    private <T extends HasMetadata> List<T> existingResources(Class<T> managedShardResource, Namespace namespace) {
        final KubernetesResourceList<T> list = kubernetesClient.resources(managedShardResource).inNamespace(namespace.getMetadata().getName()).list();
        return list == null ? Collections.emptyList() : list.getItems();
    }

}
