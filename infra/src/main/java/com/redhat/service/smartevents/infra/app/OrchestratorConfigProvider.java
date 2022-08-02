package com.redhat.service.smartevents.infra.app;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class OrchestratorConfigProvider {

    private Orchestrator orchestrator;

    @ConfigProperty(name = "event-bridge.k8s.orchestrator")
    Optional<String> orchestratorConfig;

    @PostConstruct
    void init() {
        if (orchestratorConfig.isPresent()) {
            try {
                this.orchestrator = Orchestrator.parse(orchestratorConfig.get());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        String.format("event-bridge.k8s.orchestrator configuration not recognized. Options are [%s]",
                                Arrays.stream(Orchestrator.values()).map(Orchestrator::toString).collect(Collectors.joining(","))));
            }
        }
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }
}