package com.redhat.service.smartevents.processor;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
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
        return getOptionalBean(actionValidators, actionType)
                .orElseThrow(() -> new GatewayProviderException(String.format("No validator found for action type '%s'", actionType)));
    }

    @Override
    public Optional<GatewayResolver<Action>> getActionResolver(String actionType) {
        return getOptionalBean(actionResolvers, actionType);
    }

    @Override
    public Optional<GatewayConnector<Action>> getActionConnector(String actionType) {
        return getOptionalBean(actionConnectors, actionType);
    }

    @Override
    public GatewayValidator<Source> getSourceValidator(String sourceType) {
        return getOptionalBean(sourceValidators, sourceType)
                .orElseThrow(() -> new GatewayProviderException(String.format("No validator found for source type '%s'", sourceType)));
    }

    @Override
    public GatewayResolver<Source> getSourceResolver(String sourceType) {
        return getOptionalBean(sourceResolvers, sourceType)
                .orElseThrow(() -> new GatewayProviderException(String.format("No resolver found for source type '%s'", sourceType)));
    }

    @Override
    public GatewayConnector<Source> getSourceConnector(String sourceType) {
        return getOptionalBean(sourceConnectors, sourceType)
                .orElseThrow(() -> new GatewayProviderException(String.format("No connector found for source type '%s'", sourceType)));
    }

    private static <T extends GatewayBean> Optional<T> getOptionalBean(Instance<T> instances, String sourceType) {
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

    Collection<GatewayConnector<Source>> getSourceConnectors() {
        return sourceConnectors.stream().collect(Collectors.toList());
    }
}
