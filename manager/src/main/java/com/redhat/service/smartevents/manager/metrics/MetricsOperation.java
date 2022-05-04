package com.redhat.service.smartevents.manager.metrics;

import io.micrometer.core.instrument.Tag;

/*
    Constants for the logical user API operations we are report SLI data for.
 */
public enum MetricsOperation {
    PROVISION("provision"),
    MODIFY("modify"),
    DELETE("delete");

    private final String tag;

    private MetricsOperation(String tag) {
        this.tag = tag;
    }

    Tag getMetricTag() {
        return Tag.of("operation", this.tag);
    }
}
