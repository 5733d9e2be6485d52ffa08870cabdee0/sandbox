package com.redhat.service.bridge.manager.actions.sendtobridge;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.actions.ActionTransformer;
import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.models.Bridge;

@ApplicationScoped
public class SendToBridgeActionTransformer implements ActionTransformer {

    @Inject
    BridgesService bridgesService;

    @Override
    public BaseAction transform(BaseAction action, String bridgeId, String customerId, String processorId) {
        String destinationBridgeId = action.getParameters().getOrDefault(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        Optional<Bridge> destinationBridgeOptional = bridgesService.getBridgeByIdAndCustomerId(bridgeId, customerId);
        if (destinationBridgeOptional.isEmpty()) {
            throw new ItemNotFoundException(String.format("Bridge with id '%s' for customer '%s' does not exist", bridgeId, customerId));
        }
        Bridge destinationBridge = destinationBridgeOptional.get();
        if (!bridgesService.isBridgeReady(destinationBridge)) {
            throw new BridgeLifecycleException(
                    String.format("Bridge with id '%s' for customer '%s' is not in the '%s' state.", destinationBridge.getId(), destinationBridge.getCustomerId(), BridgeStatus.READY));
        }

        Map<String, String> parameters = new HashMap<>();

        try {
            parameters.put(WebhookAction.ENDPOINT_PARAM, getBridgeWebhookUrl(destinationBridge.getEndpoint()));
            parameters.put(WebhookAction.USE_TECHNICAL_BEARER_TOKEN, "true");
        } catch (MalformedURLException e) {
            throw new ActionProviderException("Can't find events webhook for bridge " + destinationBridgeId);
        }

        BaseAction transformedAction = new BaseAction();
        transformedAction.setType(WebhookAction.TYPE);
        transformedAction.setParameters(parameters);

        return transformedAction;
    }

    static String getBridgeWebhookUrl(String bridgeEndpoint) throws MalformedURLException {
        String fullUrl = String.join("", bridgeEndpoint, bridgeEndpoint.endsWith("/") ? "" : "/", "events");
        return new URL(fullUrl).toString();
    }
}
