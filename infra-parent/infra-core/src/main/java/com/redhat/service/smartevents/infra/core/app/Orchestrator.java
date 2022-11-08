package com.redhat.service.smartevents.infra.core.app;

public enum Orchestrator {
    OPENSHIFT("openshift"),
    MINIKUBE("minikube"),
    KIND("kind");

    private String value;

    Orchestrator(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    public static Orchestrator parse(String value) {
        for (Orchestrator v : values()) {
            if (v.getValue().equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException(String.format("Orchestrator %s not valid", value));
    }
}