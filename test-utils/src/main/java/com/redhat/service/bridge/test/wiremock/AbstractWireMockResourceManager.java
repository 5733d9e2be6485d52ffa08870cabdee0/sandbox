package com.redhat.service.bridge.test.wiremock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class AbstractWireMockResourceManager implements QuarkusTestResourceLifecycleManager {

    private final String variableName;
    private WireMockServer wireMockServer;

    protected AbstractWireMockResourceManager(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        Map<String, String> variablesMap = new HashMap<>(1);
        variablesMap.put(variableName, wireMockServer.baseUrl());

        Map<String, String> configureMap = configure(wireMockServer);
        variablesMap.putAll(configureMap);

        variablesMap.forEach((key, value) -> System.err.printf("%s -> %s%n", key, value));

        return variablesMap;
    }

    protected Map<String, String> configure(WireMockServer server) {
        return Collections.emptyMap();
    }

    @Override
    public synchronized void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(wireMockServer, new TestInjector.AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class));
    }
}
