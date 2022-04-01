package com.redhat.service.bridge.processor.actions.common;

import java.util.Optional;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;

public abstract class AbstractActionAccepterFactory<T extends ActionAccepter> {

    @Inject
    Instance<T> instances;

    private final Class<T> instanceClass;

    protected AbstractActionAccepterFactory(Class<T> instanceClass) {
        this.instanceClass = instanceClass;
    }

    public T get(String actionType) {
        return getOptional(actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("No %s found for action type '%s'", instanceClass.getSimpleName(), actionType)));
    }

    public Optional<T> getOptional(String actionType) {
        return instances.stream()
                .filter(a -> a.accept(actionType))
                .findFirst();
    }
}
