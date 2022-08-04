package com.redhat.service.smartevents.manager.dns;

// At the moment we have to craft the endpoint as the fleet-shard/knative would do.
// Remove this logic when https://github.com/knative/eventing/issues/6467 is fixed and we can resolve the path directly in the Ingress/Route
// https://issues.redhat.com/browse/MGDOBR-998
public class KnativeBrokerPathBuilder {
    public static String build(String customerId, String bridgeId) {
        return String.format("/ob-%s/ob-%s", customerId, bridgeId);
    }
}
