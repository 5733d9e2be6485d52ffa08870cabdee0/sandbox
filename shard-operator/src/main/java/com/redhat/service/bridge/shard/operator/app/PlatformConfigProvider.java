package com.redhat.service.bridge.shard.operator.app;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class PlatformConfigProvider {

    private Platform platform;

    @ConfigProperty(name = "event-bridge.k8s.platform")
    String platformConfig;

    @PostConstruct
    void init() {

        if (platformConfig == null) {
            throw new IllegalStateException("event-bridge.k8s.platform configuration must be provided.");
        }

        try {
            this.platform = Platform.parse(platformConfig);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("event-bridge.k8s.platform configuration not recognized. Options are [%s]",
                            Arrays.stream(Platform.values()).map(Platform::toString).collect(Collectors.joining(","))));
        }
    }

    public Platform getPlatform() {
        return platform;
    }
}