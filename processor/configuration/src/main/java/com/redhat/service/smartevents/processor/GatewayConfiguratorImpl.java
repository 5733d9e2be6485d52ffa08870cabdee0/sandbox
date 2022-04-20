package com.redhat.service.smartevents.processor;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;

@ApplicationScoped
public class GatewayConfiguratorImpl implements GatewayConfigurator {

    @Inject
    Instance<GatewayValidator<Action>> actionValidators;
    @Inject
    Instance<GatewayResolver<Action>> actionResolvers;
    @Inject
    Instance<GatewayConnector<Action>> actionConnectors;
    @Inject
    Instance<GatewayValidator<Source>> sourceValidators;
    @Inject
    Instance<GatewayResolver<Source>> sourceResolvers;
    @Inject
    Instance<GatewayConnector<Source>> sourceConnectors;

    @Override
    public GatewayValidator<Action> getActionValidator(String actionType) {
        return getOptionalActionBean(actionValidators, actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("No validator found for action type '%s'", actionType)));
    }

    @Override
    public Optional<GatewayResolver<Action>> getActionResolver(String actionType) {
        return getOptionalActionBean(actionResolvers, actionType);
    }

    @Override
    public Optional<GatewayConnector<Action>> getActionConnector(String actionType) {
        return getOptionalActionBean(actionConnectors, actionType);
    }

    private static <T extends GatewayBean<Action>> Optional<T> getOptionalActionBean(Instance<T> instances, String sourceType) {
        return instances.stream()
                .filter(a -> a.accept(sourceType))
                .findFirst();
    }

    @Override
    public GatewayValidator<Source> getSourceValidator(String sourceType) {
        return getOptionalSourceBean(sourceValidators, sourceType)
                .orElseThrow(() -> new ActionProviderException(String.format("No validator found for source type '%s'", sourceType)));
    }

    @Override
    public GatewayResolver<Source> getSourceResolver(String sourceType) {
        return getOptionalSourceBean(sourceResolvers, sourceType)
                .orElseThrow(() -> new ActionProviderException(String.format("No resolver found for source type '%s'", sourceType)));
    }

    @Override
    public GatewayConnector<Source> getSourceConnector(String sourceType) {
        return getOptionalSourceBean(sourceConnectors, sourceType)
                .orElseThrow(() -> new ActionProviderException(String.format("No connector found for source type '%s'", sourceType)));
    }

    private static <T extends GatewayBean<Source>> Optional<T> getOptionalSourceBean(Instance<T> instances, String sourceType) {
        return instances.stream()
                .filter(a -> a.accept(sourceType))
                .findFirst();
    }

    Collection<GatewayValidator<Action>> getActionValidators() {
        return actionValidators.stream().collect(Collectors.toList());
    }

    Collection<GatewayResolver<Action>> getActionResolvers() {
        return actionResolvers.stream().collect(Collectors.toList());
    }

    Collection<GatewayConnector<Action>> getActionConnectors() {
        return actionConnectors.stream().collect(Collectors.toList());
    }

    Collection<GatewayValidator<Source>> getSourceValidators() {
        return sourceValidators.stream().collect(Collectors.toList());
    }

    Collection<GatewayResolver<Source>> getSourceResolvers() {
        return sourceResolvers.stream().collect(Collectors.toList());
    }

    Collection<GatewayConnector<Source>> getSourceConnectors() {
        return sourceConnectors.stream().collect(Collectors.toList());
    }
}
