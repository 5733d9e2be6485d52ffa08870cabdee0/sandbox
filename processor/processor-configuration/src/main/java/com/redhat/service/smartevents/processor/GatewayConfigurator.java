package com.redhat.service.smartevents.processor;

import java.util.Optional;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Source;
import com.redhat.service.smartevents.processor.resolvers.GatewayResolver;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;

public interface GatewayConfigurator {

    /**
     * Get validator bean for a gateway.
     *
     * @param actionType desired action type
     * @return the validator bean
     */
    GatewayValidator getValidator(String actionType);

    /**
     * Get resolver bean for specific action type.
     * This bean is optional and must be implemented only for non-invokable actions
     * that needs to be resolved to an invokable actions to be executed.
     *
     * @param actionType desired action type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    Optional<? extends GatewayResolver<Action>> getActionResolver(String actionType);

    /**
     * Get resolver bean for specific source type. Required for every source.
     *
     * @param sourceType desired source type
     * @return the resolver bean
     * @throws GatewayProviderException if bean is not found
     */
    GatewayResolver<Source> getSourceResolver(String sourceType);

}
