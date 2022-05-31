package com.redhat.service.smartevents.processor.actions.sendtobridge;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayResolver;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class SendToBridgeActionResolver implements SendToBridgeAction, GatewayResolver<Action> {

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public Action resolve(Action action, String customerId, String bridgeId, String processorId) {
        String destinationBridgeId = action.getParameterOrDefault(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);

        Map<String, String> parameters = new HashMap<>();
        try {
            String bridgeEndpoint = gatewayConfiguratorService.getBridgeEndpoint(destinationBridgeId, customerId);
            String bridgeWebhookUrl = getBridgeWebhookUrl(bridgeEndpoint);
            parameters.put(WebhookAction.ENDPOINT_PARAM, bridgeWebhookUrl);
            parameters.put(WebhookAction.USE_TECHNICAL_BEARER_TOKEN_PARAM, "true");
        } catch (MalformedURLException e) {
            throw new GatewayProviderException("Can't find events webhook for bridge " + destinationBridgeId);
        }

        Action transformedAction = new Action();
        transformedAction.setType(WebhookAction.TYPE);
        transformedAction.setMapParameters(parameters);

        return transformedAction;
    }

    private String getBridgeWebhookUrl(String bridgeEndpoint) throws MalformedURLException {
        return new URL(bridgeEndpoint).toString();
    }
}
