package com.redhat.service.smartevents.shard.operator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class BridgeIngressServiceImpl implements BridgeIngressService {

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public List<BridgeIngress> fetchAllBridgeIngress() {
         return kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().list().getItems();
    }
}
