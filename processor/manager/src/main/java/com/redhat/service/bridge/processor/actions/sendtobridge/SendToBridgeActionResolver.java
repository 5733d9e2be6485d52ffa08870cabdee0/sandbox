package com.redhat.service.bridge.processor.actions.sendtobridge;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.processor.actions.ActionResolver;
import com.redhat.service.bridge.processor.actions.ActionService;
import com.redhat.service.bridge.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class SendToBridgeActionResolver implements ActionResolver {

    @Inject
    ActionService actionService;

    @Override
    public String getType() {
        return SendToBridgeAction.TYPE;
    }

    @Override
    public BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId) {
        String destinationBridgeId = action.getParameters().getOrDefault(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        // Bridge destinationBridge = bridgesService.getReadyBridge();

        Map<String, String> parameters = new HashMap<>();

        try {
            parameters.put(WebhookAction.ENDPOINT_PARAM, getBridgeWebhookUrl(actionService.getBridgeEndpoint(destinationBridgeId, customerId)));
            parameters.put(WebhookAction.USE_TECHNICAL_BEARER_TOKEN_PARAM, "true");
        } catch (MalformedURLException e) {
            throw new ActionProviderException("Can't find events webhook for bridge " + destinationBridgeId);
        }

        BaseAction transformedAction = new BaseAction();
        transformedAction.setType(WebhookAction.TYPE);
        transformedAction.setParameters(parameters);

        return transformedAction;
    }

    private String getBridgeWebhookUrl(String bridgeEndpoint) throws MalformedURLException {
        return new URL(bridgeEndpoint).toString();
    }
}
