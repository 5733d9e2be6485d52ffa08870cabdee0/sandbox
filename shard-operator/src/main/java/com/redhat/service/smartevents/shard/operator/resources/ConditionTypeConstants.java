package com.redhat.service.smartevents.shard.operator.resources;

/**
 * Condition types should be named in PascalCase. Short condition names are preferred (e.g. "Ready" over "MyResourceReady").
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public class ConditionTypeConstants {
    public static final String READY = "Ready";
    public static final String AUGMENTATION = "Augmentation";
    public static final String PROGRESSING = "Progressing";
}
