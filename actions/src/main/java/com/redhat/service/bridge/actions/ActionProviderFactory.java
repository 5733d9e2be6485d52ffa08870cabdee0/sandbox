package com.redhat.service.bridge.actions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

@ApplicationScoped
public class ActionProviderFactory {

    @Inject
    Instance<ActionProvider> actionProviders;

    public ActionProvider getActionProvider(String actionType) {
        return getOptionalActionProvider(actionType)
                .orElseThrow(() -> new ActionProviderException(String.format("There is no ActionProvider recognised for type '%s'", actionType)));
    }

    public InvokableActionProvider getInvokableActionProvider(String actionType) {
        return getOptionalActionProvider(actionType)
                .filter(InvokableActionProvider.class::isInstance)
                .map(InvokableActionProvider.class::cast)
                .orElseThrow(() -> new ActionProviderException(String.format("There is no InvokableActionProvider recognised for type '%s'", actionType)));
    }

    public BaseAction resolve(BaseAction action, String bridgeId, String customerId, String processorId) {
        return getOptionalActionProvider(action.getType())
                .map(ActionProvider::getTransformer)
                .map(t -> t.transform(action, bridgeId, customerId, processorId))
                .orElseThrow(() -> new ActionProviderException(String.format("Can't resolve action of type '%s'", action.getType())));
    }

    private Optional<ActionProvider> getOptionalActionProvider(String actionType) {
        return actionProviders.stream()
                .filter((a) -> a.accept(actionType))
                .findFirst();
    }
}
