package com.redhat.service.smartevents.shard.operator.v2.utils.networking;

import io.fabric8.kubernetes.api.model.LoadBalancerIngressBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerStatusBuilder;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatus;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class V2KubernetesNetworkingTestUtils implements V2NetworkingTestUtils {

    private final KubernetesClient client;

    public V2KubernetesNetworkingTestUtils(KubernetesClient kubernetesClient) {
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
                                .withIp(V2NetworkingTestConstants.HOST_ADDRESS)
                                .build())
                        .build())
                .build();
        i.setStatus(ingressStatus);

        client.network().v1().ingresses().inNamespace(namespace).createOrReplace(i);
    }

    @Override
    public void cleanUp() {
        client.network().v1().ingresses().inAnyNamespace().delete();
    }
}
