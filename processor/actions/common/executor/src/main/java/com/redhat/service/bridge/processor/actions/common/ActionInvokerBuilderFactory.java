package com.redhat.service.bridge.processor.actions.common;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class ActionInvokerBuilderFactory {

    @Inject
    Instance<ActionInvokerBuilder> instances;

    public ActionInvokerBuilder get(String actionType) {
        return getOptional(actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("No action invoker builder found for type '%s'", actionType)));
    }

    public Optional<ActionInvokerBuilder> getOptional(String actionType) {
        return instances.stream()
                .filter(a -> a.accept(actionType))
                .findFirst();
    }
}
