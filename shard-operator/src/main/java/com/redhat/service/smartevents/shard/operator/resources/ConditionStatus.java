package com.redhat.service.smartevents.shard.operator.resources;

/**
 * Condition status values may be True, FALSE, or UNKNOWN.
 * The absence of a condition should be interpreted the same as UNKNOWN. How controllers handle UNKNOWN depends on the Condition in question.
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public enum ConditionStatus {
    TRUE,
    FALSE,
    UNKNOWN;
}
