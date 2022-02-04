package com.redhat.service.bridge.shard.operator.cucumber.common;

import java.util.HashSet;
import java.util.Random;

/**
 * Shared global context
 */
public class GlobalContext {

    /**
     * Used to make sure that generated namespace names are unique.
     */
    private static HashSet<String> usedNamespaces = new HashSet<>();
    private static Random random = new Random();

    public static synchronized String getUniqueNamespaceName() {
        // If unique namespace is not created after 10 tries then something is probably wrong, throw an exception
        for (int i = 0; i < 10; i++) {
            String generatedNamespace = String.format("bdd-%04d", random.nextInt(10000));
            if (!usedNamespaces.contains(generatedNamespace)) {
                usedNamespaces.add(generatedNamespace);
                return generatedNamespace;
            }
        }
        throw new RuntimeException("Unable to generate unique namespace name");
    }
}
