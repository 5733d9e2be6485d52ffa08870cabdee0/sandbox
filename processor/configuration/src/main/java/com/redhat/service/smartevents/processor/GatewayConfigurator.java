package com.redhat.service.smartevents.processor;

import java.util.Optional;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;

public interface GatewayConfigurator {

    /**
     * Get validator bean for specific action type. Required for every action.
     *
     * @param actionType desired action type
     * @return the validator bean
     * @throws GatewayProviderException if bean is not found
     */
    GatewayValidator<Action> getActionValidator(String actionType);

    /**
     * Get resolver bean for specific action type.
     * This bean is optional and must be implemented only for non-invokable actions
     * that needs to be resolved to an invokable actions to be executed.
     *
     * @param actionType desired action type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    GatewayResolver<Action> getActionResolver(String actionType);

    /**
     * Get connector bean for specific action type.
     * This bean is optional and must be implemented only for actions that requires
     * a Managed Connector to work.
     *
     * @param actionType desired action type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    Optional<GatewayConnector<Action>> getActionConnector(String actionType);

    /**
     * Get validator bean for specific source type. Required for every source.
     *
     * @param sourceType desired source type
     * @return the validator bean
     * @throws GatewayProviderException if bean is not found
     */
    GatewayValidator<Source> getSourceValidator(String sourceType);

    /**
     * Get resolver bean for specific source type. Required for every source.
     *
     * @param sourceType desired source type
     * @return the resolver bean
     * @throws GatewayProviderException if bean is not found
     */
    GatewayResolver<Source> getSourceResolver(String sourceType);

    /**
     * Get connector bean for specific source type. Required for every source.
     *
     * @param sourceType desired source type
     * @return the connector bean
     * @throws GatewayProviderException if bean is not found
     */
    GatewayConnector<Source> getSourceConnector(String sourceType);
}
