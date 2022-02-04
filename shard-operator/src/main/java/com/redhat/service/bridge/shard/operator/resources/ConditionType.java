package com.redhat.service.bridge.shard.operator.resources;

/**
 * Condition types should be named in PascalCase. Short condition names are preferred (e.g. "Ready" over "MyResourceReady").
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public enum ConditionType {
    Ready,
    Augmentation;
}
