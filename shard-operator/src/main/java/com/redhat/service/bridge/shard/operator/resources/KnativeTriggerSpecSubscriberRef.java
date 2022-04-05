package com.redhat.service.bridge.shard.operator.resources;

import java.util.Objects;

public class KnativeTriggerSpecSubscriberRef {
    private String apiVersion;
    private String kind;
    private String name;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnativeTriggerSpecSubscriberRef that = (KnativeTriggerSpecSubscriberRef) o;
        return Objects.equals(apiVersion, that.apiVersion) && Objects.equals(kind, that.kind) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, kind, name);
    }
}
