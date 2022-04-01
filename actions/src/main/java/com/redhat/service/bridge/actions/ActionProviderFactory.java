package com.redhat.service.bridge.actions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;

@ApplicationScoped
public class ActionProviderFactory {

    @Inject
    Instance<ActionProvider> actionProviders;

    public ActionProvider getActionProvider(String actionType) {
        return getOptionalActionProvider(actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("There is no ActionProvider recognised for type '%s'", actionType)));
    }

    private Optional<ActionProvider> getOptionalActionProvider(String actionType) {
        return actionProviders.stream()
                .filter((a) -> a.accept(actionType))
                .findFirst();
    }
}
