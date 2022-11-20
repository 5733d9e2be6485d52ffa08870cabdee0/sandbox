package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class BridgeExecutorServiceImpl implements BridgeExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeExecutorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;


    @Override
    public List<BridgeExecutor> fetchAllBridgeExecutor() {
        return kubernetesClient.resources(BridgeExecutor.class).inAnyNamespace().list().getItems();
    }
}
