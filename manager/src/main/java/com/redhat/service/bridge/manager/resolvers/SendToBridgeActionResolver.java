package com.redhat.service.bridge.manager.resolvers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.processor.actions.sendtobridge.SendToBridgeActionBean;
import com.redhat.service.bridge.processor.actions.webhook.WebhookActionBean;

@ApplicationScoped
public class SendToBridgeActionResolver implements ActionResolver {

    @Inject
    BridgesService bridgesService;

    @Override
    public String getType() {
        return SendToBridgeActionBean.TYPE;
    }

    @Override
    public BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId) {
        String destinationBridgeId = action.getParameters().getOrDefault(SendToBridgeActionBean.BRIDGE_ID_PARAM, bridgeId);
        Bridge destinationBridge = bridgesService.getReadyBridge(destinationBridgeId, customerId);

        Map<String, String> parameters = new HashMap<>();

        try {
            parameters.put(WebhookActionBean.ENDPOINT_PARAM, getBridgeWebhookUrl(destinationBridge.getEndpoint()));
            parameters.put(WebhookActionBean.USE_TECHNICAL_BEARER_TOKEN, "true");
        } catch (MalformedURLException e) {
            throw new ActionProviderException("Can't find events webhook for bridge " + destinationBridgeId);
        }

        BaseAction transformedAction = new BaseAction();
        transformedAction.setType(WebhookActionBean.TYPE);
        transformedAction.setParameters(parameters);

        return transformedAction;
    }

    private String getBridgeWebhookUrl(String bridgeEndpoint) throws MalformedURLException {
        return new URL(bridgeEndpoint).toString();
    }
}
