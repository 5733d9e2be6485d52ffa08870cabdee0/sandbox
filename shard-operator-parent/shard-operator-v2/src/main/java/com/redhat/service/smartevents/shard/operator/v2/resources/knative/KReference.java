package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import java.util.Objects;

public class KReference {

    String kind;

    String namespace;

    String name;

    String apiVersion;

    String group;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KReference that = (KReference) o;
        return Objects.equals(kind, that.kind) && Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name) && Objects.equals(apiVersion, that.apiVersion)
                && Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, namespace, name, apiVersion, group);
    }
}
