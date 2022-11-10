package com.redhat.service.smartevents.shard.operator.v1.resources.knative;

import java.util.Objects;

public class KnativeBrokerSpecConfig {
    private String config;
    private String apiVersion;
    private String kind;
    private String name;
    private String namespace;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnativeBrokerSpecConfig that = (KnativeBrokerSpecConfig) o;
        return Objects.equals(config, that.config) && Objects.equals(apiVersion, that.apiVersion) && Objects.equals(kind, that.kind) && Objects.equals(name, that.name)
                && Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(config, apiVersion, kind, name, namespace);
    }
}
