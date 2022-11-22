package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorServiceImpl implements BridgeExecutorService {

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public List<BridgeExecutor> fetchAllBridgeExecutor() {
        return kubernetesClient.resources(BridgeExecutor.class).inAnyNamespace().list().getItems();
    }

    @Override
    public BridgeExecutor fetchBridgeExecutor(String name, String namespace) {
        return kubernetesClient.resources(BridgeExecutor.class).inNamespace(namespace).withName(name).get();
    }
}
