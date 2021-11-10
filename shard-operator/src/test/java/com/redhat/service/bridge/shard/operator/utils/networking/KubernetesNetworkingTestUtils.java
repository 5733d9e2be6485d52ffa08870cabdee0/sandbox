package com.redhat.service.bridge.shard.operator.utils.networking;

import io.fabric8.kubernetes.api.model.LoadBalancerIngressBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerStatusBuilder;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatus;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesNetworkingTestUtils implements NetworkingTestUtils {

    private final KubernetesClient client;

    public KubernetesNetworkingTestUtils(KubernetesClient kubernetesClient) {
        this.client = kubernetesClient;
    }

    @Override
    public Namespaced getNetworkResource(String name, String namespace) {
        return client.network().v1().ingresses().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void patchNetworkResource(String name, String namespace) {
        Ingress i = client.network().v1().ingresses().inNamespace(namespace).withName(name).get();

        IngressStatus ingressStatus = new IngressStatusBuilder()
                .withLoadBalancer(new LoadBalancerStatusBuilder()
                        .withIngress(new LoadBalancerIngressBuilder()
                                .withHostname(name)
                                .withIp(NetworkingTestConstants.HOST_IP)
                                .build())
                        .build())
                .build();
        i.setStatus(ingressStatus);

        client.network().v1().ingresses().inNamespace(namespace).createOrReplace(i);
    }
}
