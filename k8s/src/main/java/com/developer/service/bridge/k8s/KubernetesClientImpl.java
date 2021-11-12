package com.developer.service.bridge.k8s;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.k8s.KubernetesClient;

import io.fabric8.kubernetes.api.model.apps.Deployment;

@ApplicationScoped
public class KubernetesClientImpl implements KubernetesClient {

    @Inject
    DeploymentsManager deploymentsManager;

    @Inject
    CustomResourceManager customResourceManager;

    @Override
    public void createOrUpdateDeployment(Deployment deployment) {
        deploymentsManager.createOrUpdate(deployment);
    }

    @Override
    public void deleteDeployment(String name) {
        deploymentsManager.delete(name);
    }

    @Override
    public Deployment getDeployment(String name) {
        return deploymentsManager.getDeployment(name);
    }

    @Override
    public void createOrUpdateCustomResource(String name, Object customResource, String type) {
        customResourceManager.createOrUpdateCustomResource(name, customResource, type);
    }

    @Override
    public <T> T getCustomResource(String name, Class<T> tClass) {
        return customResourceManager.getCustomResource(name, tClass);
    }

    @Override
    public void deleteCustomResource(String name, String type) {
        customResourceManager.deleteCustomResource(name, type);
    }
}
