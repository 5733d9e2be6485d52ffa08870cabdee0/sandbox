package com.redhat.service.smartevents.processor;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Source;
import com.redhat.service.smartevents.processor.models.ProcessorCatalogEntry;
import com.redhat.service.smartevents.processor.resolvers.GatewayResolver;
import com.redhat.service.smartevents.processor.resolvers.SinkConnectorResolver;
import com.redhat.service.smartevents.processor.resolvers.SourceConnectorResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.CustomGatewayResolver;
import com.redhat.service.smartevents.processor.validators.DefaultGatewayValidator;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;
import com.redhat.service.smartevents.processor.validators.custom.CustomGatewayValidator;

@ApplicationScoped
public class GatewayConfiguratorImpl implements GatewayConfigurator {

    @Inject
    Instance<CustomGatewayValidator> customValidators;

    @Inject
    Instance<CustomGatewayResolver<Action>> actionResolvers;

    @Inject
    DefaultGatewayValidator defaultGatewayValidator;

    @Inject
    SinkConnectorResolver sinkConnectorResolver;

    @Inject
    SourceConnectorResolver sourceConnectorResolver;

    @Inject
    ProcessorCatalogService processorCatalogService;

    @Override
    public GatewayValidator getValidator(String actionType) {
        Optional<CustomGatewayValidator> customValidator = getOptionalBean(customValidators, actionType);
        if (customValidator.isPresent()) {
            return customValidator.get();
        }
        return defaultGatewayValidator;
    }

    @Override
    public Optional<? extends GatewayResolver<Action>> getActionResolver(String actionType) {
        Optional<ProcessorCatalogEntry> catalogEntry = processorCatalogService.getActionsCatalog().stream().filter(x -> x.getId().equals(actionType)).findFirst();
        if (catalogEntry.isEmpty()) {
            throw new ItemNotFoundException(String.format("Action of type '%s' not recognized", actionType));
        }

        // All the connectors resolve to the SinkConnectorResolver
        if (catalogEntry.get().isConnector()) {
            return Optional.of(sinkConnectorResolver);
        }

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

    Collection<CustomGatewayResolver<Action>> getActionResolvers() {
        return actionResolvers.stream().collect(Collectors.toList());
    }
}
