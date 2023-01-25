package com.redhat.service.smartevents.shard.operator.core.resources;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Condition status values may be True, False, Unknown or Failed.
 * The absence of a condition should be interpreted the same as Unknown. How controllers handle Unknown depends on the Condition in question.
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public enum ConditionStatus {
    TRUE("True"),
    FALSE("False"),
    UNKNOWN("Unknown"),
    FAILED("Failed");

    String status;

    @JsonValue
    public String serialize() {
        return status;
    }

    ConditionStatus(String status) {
        this.status = status;
    }
}
