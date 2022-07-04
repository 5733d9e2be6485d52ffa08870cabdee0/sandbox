package com.redhat.service.smartevents.shard.operator.resources;

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
public class ConditionReasonConstants {
    public static final String DEPLOYMENT_AVAILABLE = "DeploymentAvailable";
    public static final String DEPLOYMENT_PROGRESSING = "DeploymentProgressing";
    public static final String DEPLOYMENT_FAILED = "DeploymentFailed";
    public static final String DEPLOYMENT_NOT_AVAILABLE = "DeploymentNotAvailable";
    public static final String SERVICE_NOT_READY = "ServiceNotReady";
    public static final String NETWORK_RESOURCE_NOT_READY = "NetworkResourceNotReady";
    public static final String KNATIVE_BROKER_NOT_READY = "KnativeBrokerNotReady";
    public static final String PROMETHEUS_UNAVAILABLE = "PrometheusUnavailable";
    public static final String SECRETS_NOT_FOUND = "SecretsNotFound";
    public static final String CAMEL_NOT_READY = "CamelNotReady";
}
