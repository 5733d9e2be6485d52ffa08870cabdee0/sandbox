package com.redhat.service.bridge.processor.actions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;

@ApplicationScoped
public class ActionConfiguratorImpl implements ActionConfigurator {

    @Inject
    Instance<ActionValidator> validators;
    @Inject
    Instance<ActionResolver> resolvers;
    @Inject
    Instance<ActionConnector> connectors;

    @Override
    public ActionValidator getValidator(String actionType) {
        return getOptional(validators, actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("No validator found for action type '%s'", actionType)));
    }

    @Override
    public Optional<ActionResolver> getResolver(String actionType) {
        return getOptional(resolvers, actionType);
    }

    @Override
    public Optional<ActionConnector> getConnector(String actionType) {
        return getOptional(connectors, actionType);
    }

    private static <T extends ActionBean> Optional<T> getOptional(Instance<T> instances, String actionType) {
        return instances.stream()
                .filter(a -> a.accept(actionType))
                .findFirst();
    }
}
