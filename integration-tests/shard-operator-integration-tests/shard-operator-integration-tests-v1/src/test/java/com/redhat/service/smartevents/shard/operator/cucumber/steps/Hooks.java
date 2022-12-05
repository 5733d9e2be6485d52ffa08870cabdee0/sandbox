package com.redhat.service.smartevents.shard.operator.cucumber.steps;

import java.io.IOException;

import com.redhat.service.smartevents.integration.tests.context.ShardContext;
import com.redhat.service.smartevents.integration.tests.logs.EventCollector;
import com.redhat.service.smartevents.integration.tests.logs.LogCollector;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber hooks for setup and cleanup
 */
public class Hooks {

    private ShardContext context;

    public Hooks(ShardContext context) {
        this.context = context;
    }

    @Before
    public void before(Scenario scenario) {
        this.context.setScenario(scenario);
    }

    @After(order = 10)
    public void cleanupTestNamespaceAfterScenario() {
        context.getClient().namespaces().withName(context.getNamespace()).delete();
    }

    @After(order = 20)
    public void storeTestNamespaceLogs() throws IOException {
        LogCollector.storeNamespacePodLogs(context.getClient(), String.format("%s-%s", context.getScenario().getName(), context.getNamespace()), context.getNamespace());
        EventCollector.storeNamespaceEvents(context.getClient(), String.format("%s-%s", context.getScenario().getName(), context.getNamespace()), context.getNamespace());
    }
}
