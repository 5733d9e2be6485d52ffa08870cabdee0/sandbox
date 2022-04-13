package com.redhat.service.smartevents.processor.sources;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;

@ApplicationScoped
public class SourceConfiguratorImpl implements SourceConfigurator {

    @Inject
    Instance<SourceValidator> validators;
    @Inject
    Instance<SourceResolver> resolvers;
    @Inject
    Instance<SourceConnector> connectors;

    @Override
    public SourceValidator getValidator(String sourceType) {
        return getOptional(validators, sourceType)
                .orElseThrow(() -> new ActionProviderException(String.format("No validator found for source type '%s'", sourceType)));
    }

    @Override
    public Optional<SourceResolver> getResolver(String sourceType) {
        return getOptional(resolvers, sourceType);
    }

    @Override
    public Optional<SourceConnector> getConnector(String sourceType) {
        return getOptional(connectors, sourceType);
    }

    private static <T extends SourceBean> Optional<T> getOptional(Instance<T> instances, String sourceType) {
        return instances.stream()
                .filter(a -> a.accept(sourceType))
                .findFirst();
    }

    Collection<SourceValidator> getValidators() {
        return validators.stream().collect(Collectors.toList());
    }

    Collection<SourceResolver> getResolvers() {
        return resolvers.stream().collect(Collectors.toList());
    }

    Collection<SourceConnector> getConnectors() {
        return connectors.stream().collect(Collectors.toList());
    }
}
