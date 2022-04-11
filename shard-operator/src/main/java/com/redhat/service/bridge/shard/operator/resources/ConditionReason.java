package com.redhat.service.bridge.shard.operator.resources;

/**
 * In condition types, and everywhere else they appear in the API, Reason is intended to be a one-word,
 * CamelCase representation of the category of cause of the current status, and Message is intended to be a human-readable phrase or sentence,
 * which may contain specific details of the individual occurrence. Reason is intended to be used in concise output,
 * such as one-line kubectl get output, and in summarizing occurrences of causes, whereas Message is intended to be presented to users in detailed status explanations,
 * such as kubectl describe output.
 *
 * @see <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions - Typical Status
 *      Properties</a>
 */
public enum ConditionReason {
    DeploymentAvailable,
    DeploymentProgressing,
    DeploymentFailed,
    DeploymentNotAvailable,
    ServiceNotReady,
    KnativeBrokerNotReady,
    PrometheusUnavailable,
    SecretsNotFound;
}
