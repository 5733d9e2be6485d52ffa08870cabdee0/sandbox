package com.redhat.service.smartevents.shard.operator.app;

public enum Platform {
    OPENSHIFT("openshift"),
    KUBERNETES("k8s");

    private String value;

    Platform(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    public static Platform parse(String value) {
        for (Platform v : values()) {
            if (v.getValue().equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException(String.format("Platform %s not valid", value));
    }
}