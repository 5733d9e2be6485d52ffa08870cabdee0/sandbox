package com.redhat.service.smartevents.processor;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.smartevents.processor.actions.ActionBean;
import com.redhat.service.smartevents.processor.actions.ActionConnector;
import com.redhat.service.smartevents.processor.actions.ActionResolver;
import com.redhat.service.smartevents.processor.actions.ActionValidator;
import com.redhat.service.smartevents.processor.sources.SourceBean;
import com.redhat.service.smartevents.processor.sources.SourceConnector;
import com.redhat.service.smartevents.processor.sources.SourceResolver;
import com.redhat.service.smartevents.processor.sources.SourceValidator;

@ApplicationScoped
public class GatewayConfiguratorImpl implements GatewayConfigurator {

    @Inject
    Instance<ActionValidator> actionValidators;
    @Inject
    Instance<ActionResolver> actionResolvers;
    @Inject
    Instance<ActionConnector> actionConnectors;
    @Inject
    Instance<SourceValidator> sourceValidators;
    @Inject
    Instance<SourceResolver> sourceResolvers;
    @Inject
    Instance<SourceConnector> sourceConnectors;

    @Override
    public ActionValidator getActionValidator(String actionType) {
        return getOptionalActionBean(actionValidators, actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("No validator found for action type '%s'", actionType)));
    }

    @Override
    public Optional<ActionResolver> getActionResolver(String actionType) {
        return getOptionalActionBean(actionResolvers, actionType);
    }

    @Override
    public Optional<ActionConnector> getActionConnector(String actionType) {
        return getOptionalActionBean(actionConnectors, actionType);
    }

    private static <T extends ActionBean> Optional<T> getOptionalActionBean(Instance<T> instances, String sourceType) {
        return instances.stream()
                .filter(a -> a.accept(sourceType))
                .findFirst();
    }

    @Override
    public SourceValidator getSourceValidator(String sourceType) {
        return getOptionalSourceBean(sourceValidators, sourceType)
                .orElseThrow(() -> new ActionProviderException(String.format("No validator found for source type '%s'", sourceType)));
    }

    @Override
    public SourceResolver getSourceResolver(String sourceType) {
        return getOptionalSourceBean(sourceResolvers, sourceType)
                .orElseThrow(() -> new ActionProviderException(String.format("No resolver found for source type '%s'", sourceType)));
    }

    @Override
    public SourceConnector getSourceConnector(String sourceType) {
        return getOptionalSourceBean(sourceConnectors, sourceType)
                .orElseThrow(() -> new ActionProviderException(String.format("No connector found for source type '%s'", sourceType)));
    }

    private static <T extends SourceBean> Optional<T> getOptionalSourceBean(Instance<T> instances, String sourceType) {
        return instances.stream()
                .filter(a -> a.accept(sourceType))
                .findFirst();
    }

    Collection<ActionValidator> getActionValidators() {
        return actionValidators.stream().collect(Collectors.toList());
    }

    Collection<ActionResolver> getActionResolvers() {
        return actionResolvers.stream().collect(Collectors.toList());
    }

    Collection<ActionConnector> getActionConnectors() {
        return actionConnectors.stream().collect(Collectors.toList());
    }
}
