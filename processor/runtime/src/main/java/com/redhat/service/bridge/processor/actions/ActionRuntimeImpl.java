package com.redhat.service.bridge.processor.actions;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;

@ApplicationScoped
public class ActionRuntimeImpl implements ActionRuntime {

    @Inject
    Instance<ActionInvokerBuilder> invokerBuilders;

    @Override
    public ActionInvokerBuilder getInvokerBuilder(String actionType) {
        return invokerBuilders.stream()
                .filter(a -> a.accept(actionType))
                .findFirst()
                .orElseThrow(() -> new ActionProviderException(String.format("No invoker builder found for action type '%s'", actionType)));
    }

    Collection<ActionInvokerBuilder> getInvokerBuilders() {
        return invokerBuilders.stream().collect(Collectors.toList());
    }
}
