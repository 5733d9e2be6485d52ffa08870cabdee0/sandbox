package com.redhat.service.bridge.shard.operator;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class CleanUpServiceImpl implements CleanUpService {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Override
    // Scheduled at midnight
    @Scheduled(cron = "0 0 0 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void cleanUpEmptyNamespaces() {
        List<Namespace> namespaces = kubernetesClient
                .namespaces()
                .list()
                .getItems()
                .stream()
                .filter(ns -> ns.getMetadata().getName().startsWith(CustomerNamespaceProvider.NS_PREFIX_FORMAT))
                .collect(Collectors.toList());
        for (Namespace namespace : namespaces) {
            customerNamespaceProvider.deleteNamespaceIfEmpty(namespace);
        }
    }
}
