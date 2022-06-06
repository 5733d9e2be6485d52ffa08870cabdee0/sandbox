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
import com.redhat.service.smartevents.processor.sources.SourceResolver;

@ApplicationScoped
public class GatewayConfiguratorImpl implements GatewayConfigurator {

    @Inject
    Instance<GatewayValidator<Action>> actionValidators;
    @Inject
    Instance<GatewayResolver<Action>> actionResolvers;
    @Inject
    Instance<GatewayValidator<Source>> sourceValidators;

    @Inject
    SourceResolver sourceResolver;

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
    public GatewayValidator<Source> getSourceValidator(String sourceType) {
        return getOptionalBean(sourceValidators, sourceType)
                .orElseThrow(() -> new GatewayProviderException(String.format("No validator found for source type '%s'", sourceType)));
    }

    @Override
    public GatewayResolver<Source> getSourceResolver(String sourceType) {
        return sourceResolver;
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

    Collection<GatewayValidator<Source>> getSourceValidators() {
        return sourceValidators.stream().collect(Collectors.toList());
    }
}
