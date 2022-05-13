package com.redhat.service.smartevents.processor.sources.slack;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.processor.SensitiveParamGatewayResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayResolver;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class SlackSourceResolver extends SensitiveParamGatewayResolver<Source> implements SlackSource {

    private static final Logger LOG = LoggerFactory.getLogger(SlackSourceResolver.class);

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    protected Action resolveActionWithoutSensitiveParameters(Source gateway, String customerId, String bridgeId, String processorId) {
        Action resolvedAction = new Action();
        resolvedAction.setType(WebhookAction.TYPE);
        resolvedAction.setParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, getBridgeWebhookUrl(customerId, bridgeId),
                WebhookAction.USE_TECHNICAL_BEARER_TOKEN_PARAM, "true"));
        return resolvedAction;
    }

    @Override
    protected Set<String> getSensitiveParameterNames() {
        return Set.of(SlackSource.TOKEN_PARAM);
    }

    private String getBridgeWebhookUrl(String customerId, String bridgeId) {
        try {
            return new URL(gatewayConfiguratorService.getBridgeEndpoint(bridgeId, customerId)).toString();
        } catch (MalformedURLException e) {
            LOG.error("MalformedURLException in SlackSourceResolver", e);
            throw new GatewayProviderException("Can't find events webhook for bridge " + bridgeId);
        }
    }
}
