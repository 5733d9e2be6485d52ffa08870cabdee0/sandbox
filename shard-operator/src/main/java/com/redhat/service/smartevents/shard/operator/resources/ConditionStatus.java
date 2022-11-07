package com.redhat.service.smartevents.shard.operator.resources;

/**
 * Condition status values may be TRUE, FALSE, or Unknown.
 * The absence of a condition should be interpreted the same as Unknown. How controllers handle Unknown depends on the Condition in question.
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public enum ConditionStatus {
    TRUE,
    FALSE,
    Unknown;

}
