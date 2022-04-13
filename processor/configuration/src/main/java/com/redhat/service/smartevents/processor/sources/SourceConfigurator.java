package com.redhat.service.smartevents.processor.sources;

import java.util.Optional;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;

public interface SourceConfigurator {

    /**
     * Get validator bean for specific source type. Required for every source.
     *
     * @param sourceType desired source type
     * @return the validator bean
     * @throws ActionProviderException if bean is not found
     */
    SourceValidator getValidator(String sourceType);

    /**
     * Get resolver bean for specific source type.
     * This bean is optional and must be implemented only for non-invokable sources
     * that needs to be resolved to an invokable sources to be executed.
     *
     * @param sourceType desired source type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    Optional<SourceResolver> getResolver(String sourceType);

    /**
     * Get connector bean for specific source type.
     * This bean is optional and must be implemented only for sources that requires
     * a Managed Connector to work.
     *
     * @param sourceType desired source type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    Optional<SourceConnector> getConnector(String sourceType);
}
