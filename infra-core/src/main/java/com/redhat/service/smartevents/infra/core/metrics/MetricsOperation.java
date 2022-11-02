package com.redhat.service.smartevents.infra.core.metrics;

import io.micrometer.core.instrument.Tag;

/*
    Constants for the logical user API operations we are report SLI data for.
 */
public enum MetricsOperation {
    MANAGER_RESOURCE_PROVISION("provision"),
    MANAGER_RESOURCE_MODIFY("modify"),
    MANAGER_RESOURCE_DELETE("delete"),
    OPERATOR_RESOURCE_PROVISION("operator_provision"),
    OPERATOR_RESOURCE_DELETE("operator_delete"),
    CONTROLLER_RESOURCE_PROVISION("controller_provision"),
    CONTROLLER_RESOURCE_DELETE("controller_delete"),
    OPERATOR_MANAGER_FETCH("fetch"),
    OPERATOR_MANAGER_UPDATE("update");

    private final String tag;

    MetricsOperation(String tag) {
        this.tag = tag;
    }

    public Tag getMetricTag() {
        return Tag.of("operation", this.tag);
    }
}
