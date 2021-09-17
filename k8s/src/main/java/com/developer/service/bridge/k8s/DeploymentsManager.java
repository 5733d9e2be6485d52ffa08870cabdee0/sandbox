package com.developer.service.bridge.k8s;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface DeploymentsManager {

    void createOrUpdate(Deployment deployment);

    void delete(String name);

    Deployment getDeployment(String name);
}
