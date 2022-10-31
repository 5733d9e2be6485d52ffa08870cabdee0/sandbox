package com.redhat.service.smartevents.processor;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.models.ProcessorCatalogEntry;
import com.redhat.service.smartevents.processor.resolvers.ActionResolver;
import com.redhat.service.smartevents.processor.resolvers.SinkConnectorResolver;
import com.redhat.service.smartevents.processor.resolvers.custom.CustomActionResolver;
import com.redhat.service.smartevents.processor.validators.ActionValidator;
import com.redhat.service.smartevents.processor.validators.DefaultActionValidator;
import com.redhat.service.smartevents.processor.validators.custom.CustomActionValidator;

@ApplicationScoped
public class ActionConfiguratorImpl implements ActionConfigurator {

    @Inject
    Instance<CustomActionValidator> customValidators;

    @Inject
    Instance<CustomActionResolver<Action>> actionResolvers;

    @Inject
    DefaultActionValidator defaultGatewayValidator;

    @Inject
    SinkConnectorResolver sinkConnectorResolver;

    @Inject
    ProcessorCatalogService processorCatalogService;

    @Override
    public ActionValidator getValidator(String actionType) {
        Optional<CustomActionValidator> customValidator = getOptionalBean(customValidators, actionType);
        if (customValidator.isPresent()) {
            return customValidator.get();
        }
        return defaultGatewayValidator;
    }

    @Override
    public Optional<? extends ActionResolver> getActionResolver(String actionType) {
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

    private static <T extends GatewayBean> Optional<T> getOptionalBean(Instance<T> instances, String sourceType) {
        return instances.stream()
                .filter(a -> a.accept(sourceType))
                .findFirst();
    }

    Collection<CustomActionResolver<Action>> getActionResolvers() {
        return actionResolvers.stream().collect(Collectors.toList());
    }
}
