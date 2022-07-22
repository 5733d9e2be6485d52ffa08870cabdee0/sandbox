package com.redhat.service.smartevents.infra.exceptions;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.UnspecifiedProvisioningFailureException;

import io.quarkus.runtime.Quarkus;

@ApplicationScoped
public class BridgeErrorHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeErrorHelper.class);

    @Inject
    BridgeErrorService bridgeErrorService;

    BridgeError deploymentFailedException;

    @PostConstruct
    protected void setup() {
        Optional<BridgeError> error = bridgeErrorService.getError(UnspecifiedProvisioningFailureException.class);
        if (error.isPresent()) {
            deploymentFailedException = error.get();
        } else {
            LOGGER.error("{} error is not defined in the ErrorsService.", UnspecifiedProvisioningFailureException.class.getSimpleName());
            Quarkus.asyncExit(1);
        }
    }

    public BridgeError getBridgeError(Exception e) {
        LOGGER.info("Mapping '{}' to BridgeError", e.getClass().getName(), e);
        return bridgeErrorService.getError(e.getClass())
                .map((b) -> {
                    LOGGER.info("Mapped '{}' to '{}'", e.getClass().getName(), b);
                    if (e instanceof ProvidesReason) {
                        return new BridgeError(b.getId(), b.getCode(), e.getMessage(), b.getType());
                    }
                    return b;
                })
                .orElseGet(() -> {
                    LOGGER.info("'{}' not found in error catalog. Falling back to generic UnspecifiedProvisioningFailureException.", e.getClass().getName());
                    return deploymentFailedException;
                });
    }

}
