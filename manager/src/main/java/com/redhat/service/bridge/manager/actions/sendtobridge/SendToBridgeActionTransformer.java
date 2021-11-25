package com.redhat.service.bridge.manager.actions.sendtobridge;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.actions.ActionTransformer;
import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.models.Bridge;

@ApplicationScoped
public class SendToBridgeActionTransformer implements ActionTransformer {

    @Inject
    BridgesService bridgesService;

    @Override
    public BaseAction transform(BaseAction action, String bridgeId, String customerId) {
        String destinationBridgeId = action.getParameters().getOrDefault(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        Bridge destinationBridge = bridgesService.getAvailableBridge(destinationBridgeId, customerId);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(WebhookAction.ENDPOINT_PARAM, destinationBridge.getEndpoint());

        BaseAction transformedAction = new BaseAction();
        transformedAction.setType(WebhookAction.TYPE);
        transformedAction.setName(action.getName());
        transformedAction.setParameters(parameters);

        return transformedAction;
    }
}
