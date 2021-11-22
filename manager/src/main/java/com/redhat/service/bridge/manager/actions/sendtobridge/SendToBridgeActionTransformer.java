package com.redhat.service.bridge.manager.actions.sendtobridge;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.actions.ActionTransformer;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.models.Bridge;

@ApplicationScoped
public class SendToBridgeActionTransformer implements ActionTransformer {

    @Inject
    BridgesService bridgesService;

    @Override
    public String getType() {
        return SendToBridgeAction.TYPE;
    }

    @Override
    public BaseAction transform(Bridge bridge, String customerId, ProcessorRequest processorRequest) {
        BaseAction action = processorRequest.getAction();

        Map<String, String> parameters = new HashMap<>();
        if (!action.getParameters().containsKey(SendToBridgeAction.BRIDGE_ID_PARAM)) {
            parameters.put(WebhookAction.ENDPOINT_PARAM, bridge.getEndpoint());
        } else {
            Bridge otherBridge = bridgesService.getAvailableBridge(action.getParameters().get(SendToBridgeAction.BRIDGE_ID_PARAM), customerId);
            parameters.put(WebhookAction.ENDPOINT_PARAM, otherBridge.getEndpoint());
        }

        BaseAction transformedAction = new BaseAction();
        transformedAction.setType(WebhookAction.TYPE);
        transformedAction.setName(action.getName());
        transformedAction.setParameters(parameters);

        return transformedAction;
    }
}
