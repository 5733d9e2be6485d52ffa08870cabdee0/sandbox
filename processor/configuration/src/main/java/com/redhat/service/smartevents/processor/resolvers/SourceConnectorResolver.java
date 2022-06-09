package com.redhat.service.smartevents.processor.resolvers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayResolver;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class SourceConnectorResolver implements GatewayResolver<Source> {

    private static final Logger LOG = LoggerFactory.getLogger(SourceConnectorResolver.class);

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public Action resolve(Source source, String customerId, String bridgeId, String processorId) {
        Action resolvedAction = new Action();
        resolvedAction.setType(WebhookAction.TYPE);
        resolvedAction.setMapParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, getBridgeWebhookUrl(customerId, bridgeId),
                WebhookAction.USE_TECHNICAL_BEARER_TOKEN_PARAM, "true"));
        return resolvedAction;
    }

    private String getBridgeWebhookUrl(String customerId, String bridgeId) {
        try {
            return new URL(gatewayConfiguratorService.getBridgeEndpoint(bridgeId, customerId)).toString();
        } catch (MalformedURLException e) {
            LOG.error("MalformedURLException in SlackSourceResolver", e);
            throw new GatewayProviderException("Can't find events webhook for bridge " + bridgeId);
        }
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public boolean accept(String gatewayType) {
        return true;
    }
}
