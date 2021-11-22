package com.redhat.service.bridge.shard.operator.cucumber.steps;

import java.io.FileNotFoundException;

import com.redhat.service.bridge.shard.operator.cucumber.common.Context;

import io.cucumber.java.After;

/**
 * Cucumber hooks for setup and cleanup
 */
public class Hooks {

    private Context context;

    public Hooks(Context context) {
        this.context = context;
    }

    @After
    public void cleanupNamespaceAfterScenario() throws FileNotFoundException {
        context.getClient().namespaces().withName(context.getNamespace()).delete();
    }
}
