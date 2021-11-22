package com.redhat.service.bridge.shard.operator.cucumber.common;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * Shared scenario context
 */
public class Context {

    private String namespace;
    private OpenShiftClient oc;

    public Context() {
        namespace = GlobalContext.getUniqueNamespaceName();
        oc = new DefaultOpenShiftClient();
    }

    public String getNamespace() {
        return namespace;
    }

    public OpenShiftClient getClient() {
        return oc;
    }
}
