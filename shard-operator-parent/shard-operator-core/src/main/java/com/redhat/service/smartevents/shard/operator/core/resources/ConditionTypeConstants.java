package com.redhat.service.smartevents.shard.operator.core.resources;

/**
 * Condition types should be named in PascalCase. Short condition names are preferred (e.g. "Ready" over "MyResourceReady").
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public class ConditionTypeConstants {
    private ConditionTypeConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String READY = "Ready";
}
