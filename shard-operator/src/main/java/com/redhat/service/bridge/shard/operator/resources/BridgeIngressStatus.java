package com.redhat.service.bridge.shard.operator.resources;

import java.util.HashSet;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;

/**
 * To be defined on <a href="MGDOBR-91">https://issues.redhat.com/browse/MGDOBR-91</a>
 * <p>
 * This status MUST implement the status best practices as defined by the
 * <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions</a>
 */
public class BridgeIngressStatus extends CustomResourceStatus {

    private static final HashSet<Condition> INGRESS_CONDITIONS = new HashSet<Condition>() {
        {
            add(new Condition(ConditionType.Ready, ConditionStatus.Unknown));
            add(new Condition(ConditionType.Augmentation, ConditionStatus.Unknown));
        }
    };

    private String endpoint;

    public BridgeIngressStatus() {
        super(INGRESS_CONDITIONS);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
