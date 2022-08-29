package com.redhat.service.smartevents.infra.app;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OrchestratorConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorConfigProvider.class);

    private Orchestrator orchestrator;

    @ConfigProperty(name = "event-bridge.k8s.orchestrator")
    Optional<String> orchestratorConfig;

    @PostConstruct
    void init() {
        if (orchestratorConfig.isPresent()) {
            LOGGER.info("Selected orchestrator is '{}'", orchestratorConfig.get());
            try {
                this.orchestrator = Orchestrator.parse(orchestratorConfig.get());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        String.format("event-bridge.k8s.orchestrator configuration not recognized. Options are [%s]",
                                Arrays.stream(Orchestrator.values()).map(Orchestrator::toString).collect(Collectors.joining(","))));
            }
        } else {
            LOGGER.info("No orchestrator configuration was provided.");
        }
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }
}