package com.redhat.service.smartevents.processor.sources.slack;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayResolver;
import com.redhat.service.smartevents.processor.actions.input.InputAction;

@ApplicationScoped
public class SlackSourceResolver implements SlackSource, GatewayResolver<Source> {

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public Action resolve(Source source, String customerId, String bridgeId, String processorId) {
        Action resolvedAction = new Action();
        resolvedAction.setType(InputAction.TYPE);

        try {
            resolvedAction.setParameters(Map.of(
                    InputAction.ENDPOINT_PARAM, getBridgeWebhookUrl(customerId, bridgeId),
                    InputAction.CLOUD_EVENT_TYPE, "SlackSource"));
        } catch (MalformedURLException e) {
            throw new GatewayProviderException("Can't find events webhook for bridge " + bridgeId);
        }

        return resolvedAction;
    }

    private String getBridgeWebhookUrl(String customerId, String bridgeId) throws MalformedURLException {
        return new URL(gatewayConfiguratorService.getBridgeEndpoint(bridgeId, customerId)).toString();
    }
}
