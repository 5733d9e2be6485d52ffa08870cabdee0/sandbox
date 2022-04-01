package com.redhat.service.bridge.manager.resolvers;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;

@ApplicationScoped
public class ActionResolverFactory {

    @Inject
    Instance<ActionResolver> instances;

    public ActionResolver get(String actionType) {
        return getOptional(actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("No action resolver found for type '%s'", actionType)));
    }

    public Optional<ActionResolver> getOptional(String actionType) {
        return instances.stream()
                .filter(a -> a.accept(actionType))
                .findFirst();
    }
}
