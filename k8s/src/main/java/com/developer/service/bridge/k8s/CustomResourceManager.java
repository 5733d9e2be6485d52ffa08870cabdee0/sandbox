package com.developer.service.bridge.k8s;

public interface CustomResourceManager {
    void createOrUpdateCustomResource(String name, Object customResource, String type);

    <T> T getCustomResource(String name, Class<T> tClass);

    void deleteCustomResource(String name, String type);
}
