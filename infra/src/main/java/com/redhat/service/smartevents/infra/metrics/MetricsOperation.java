package com.redhat.service.smartevents.infra.metrics;

import io.micrometer.core.instrument.Tag;

/*
    Constants for the logical user API operations we are report SLI data for.
 */
public enum MetricsOperation {
    RESOURCE_PROVISION("provision"),
    RESOURCE_MODIFY("modify"),
    RESOURCE_DELETE("delete"),
    MANAGER_FETCH("fetch"),
    MANAGER_UPDATE("update");

    private final String tag;

    MetricsOperation(String tag) {
        this.tag = tag;
    }

    public Tag getMetricTag() {
        return Tag.of("operation", this.tag);
    }
}
