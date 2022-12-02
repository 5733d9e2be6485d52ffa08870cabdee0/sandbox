package com.redhat.service.smartevents.integration.tests.context;

import io.cucumber.java.Scenario;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * Shared scenario context
 */
public class ShardContext {

    private Scenario scenario;
    private String namespace;
    private OpenShiftClient oc;

    public ShardContext() {
        namespace = GlobalContext.getUniqueNamespaceName();
        oc = new DefaultOpenShiftClient();
    }

    public String getNamespace() {
        return namespace;
    }

    public OpenShiftClient getClient() {
        return oc;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }
}
