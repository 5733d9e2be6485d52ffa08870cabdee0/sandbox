package com.redhat.service.bridge.shard.operator.resources;

/**
 * Condition status values may be True, False, or Unknown.
 * The absence of a condition should be interpreted the same as Unknown. How controllers handle Unknown depends on the Condition in question.
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public enum ConditionStatus {
    True,
    False,
    Unknown;
}
