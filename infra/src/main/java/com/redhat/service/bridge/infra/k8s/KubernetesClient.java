package com.redhat.service.bridge.infra.k8s;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

public interface KubernetesClient {
    void createOrUpdateDeployment(Deployment deployment);

    void deleteDeployment(String name);

    Deployment getDeployment(String name);

    void createOrUpdateCustomResource(String name, Object customResource, String type);

    <T> T getCustomResource(String name, Class<T> tClass);

    void deleteCustomResource(String name, String type);

    void createNetworkIngress(Ingress ingress);

    void deleteNetworkIngress(String name);

    Ingress getNetworkIngress(String name);
}
