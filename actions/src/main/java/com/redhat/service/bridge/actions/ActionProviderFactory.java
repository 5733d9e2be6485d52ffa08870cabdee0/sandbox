package com.redhat.service.bridge.actions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class ActionProviderFactory {

    @Inject
    Instance<ActionProvider> actionProviders;

    public ActionProvider getActionProvider(String actionType) {
        Optional<ActionProvider> ap = actionProviders.stream().filter((a) -> a.accept(actionType)).findFirst();
        if (ap.isPresent()) {
            return ap.get();
        }

        throw new ActionProviderException(String.format("There is no ActionProvider recognised for type '%s'", actionType));
    }
}
