package com.redhat.service.rhose.processor.actions;

import java.util.Optional;

import com.redhat.service.rhose.infra.exceptions.definitions.user.ActionProviderException;

public interface ActionConfigurator {

    /**
     * Get validator bean for specific action type. Required for every action.
     *
     * @param actionType desired action type
     * @return the validator bean
     * @throws ActionProviderException if bean is not found
     */
    ActionValidator getValidator(String actionType);

    /**
     * Get resolver bean for specific action type.
     * This bean is optional and must be implemented only for non-invokable actions
     * that needs to be resolved to an invokable actions to be executed.
     *
     * @param actionType desired action type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    Optional<ActionResolver> getResolver(String actionType);

    /**
     * Get connector bean for specific action type.
     * This bean is optional and must be implemented only for actions that requires
     * a Managed Connector to work.
     *
     * @param actionType desired action type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    Optional<ActionConnector> getConnector(String actionType);
}
