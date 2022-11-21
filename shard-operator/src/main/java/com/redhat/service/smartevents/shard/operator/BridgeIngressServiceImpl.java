package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public List<BridgeIngress> fetchAllBridgeIngress() {
         return kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().list().getItems();
    }

    @Override
    public BridgeIngress fetchBridgeIngress(String name, String namespace) {
        return kubernetesClient.resources(BridgeIngress.class).inNamespace(namespace).withName(name).get();
    }
}
