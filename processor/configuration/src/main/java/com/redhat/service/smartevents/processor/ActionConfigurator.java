package com.redhat.service.smartevents.processor;

import java.util.Optional;

import com.redhat.service.smartevents.processor.resolvers.ActionResolver;
import com.redhat.service.smartevents.processor.validators.ActionValidator;

public interface ActionConfigurator {

    /**
     * Get validator bean for a gateway.
     *
     * @param actionType desired action type
     * @return the validator bean
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
    Optional<? extends ActionResolver> getActionResolver(String actionType);

}
