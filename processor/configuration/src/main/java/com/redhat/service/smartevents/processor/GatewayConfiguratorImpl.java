package com.redhat.service.smartevents.processor;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.resolvers.SourceConnectorResolver;
import com.redhat.service.smartevents.processor.validators.DefaultGatewayValidator;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;
import com.redhat.service.smartevents.processor.validators.custom.CustomGatewayValidator;

@ApplicationScoped
public class GatewayConfiguratorImpl implements GatewayConfigurator {

    @Inject
    Instance<CustomGatewayValidator> customValidators;

    @Inject
    Instance<GatewayResolver<Action>> actionResolvers;

    @Inject
    DefaultGatewayValidator defaultGatewayValidator;

    @Inject
    SourceConnectorResolver sourceConnectorResolver;

    @Override
    public GatewayValidator getValidator(String actionType) {
        Optional<CustomGatewayValidator> customValidator = getOptionalBean(customValidators, actionType);
        if (customValidator.isPresent()) {
            return customValidator.get();
        }
        return defaultGatewayValidator;
    }

    @Override
    public Optional<GatewayResolver<Action>> getActionResolver(String actionType) {
        return getOptionalBean(actionResolvers, actionType);
    }

    @Override
    public GatewayResolver<Source> getSourceResolver(String sourceType) {
        return sourceConnectorResolver;
    }

    private static <T extends GatewayBean> Optional<T> getOptionalBean(Instance<T> instances, String sourceType) {
        return instances.stream()
                .filter(a -> a.accept(sourceType))
                .findFirst();
    }

    Collection<CustomGatewayValidator> getCustomValidators() {
        return customValidators.stream().collect(Collectors.toList());
    }

    Collection<GatewayResolver<Action>> getActionResolvers() {
        return actionResolvers.stream().collect(Collectors.toList());
    }
}
