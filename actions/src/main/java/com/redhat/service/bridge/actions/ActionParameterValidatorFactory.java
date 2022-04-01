package com.redhat.service.bridge.actions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;

@ApplicationScoped
public class ActionParameterValidatorFactory {

    @Inject
    Instance<ActionParameterValidator> instances;

    public ActionParameterValidator get(String actionType) {
        return getOptional(actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("There is no ActionProvider recognised for type '%s'", actionType)));
    }

    public Optional<ActionParameterValidator> getOptional(String actionType) {
        return instances.stream()
                .filter(a -> a.accept(actionType))
                .findFirst();
    }
}
