package com.developer.service.bridge.k8s;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

public interface NetworkManager {
    void createOrUpdate(Ingress ingress);

    void delete(String name);

    Ingress getIngress(String name);
}
