package com.redhat.service.smartevents.processor.sources.aws;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.SensitiveParamGatewayResolver;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class AwsSqsSourceResolver extends SensitiveParamGatewayResolver<Source> implements AwsSqsSource {

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
        return Set.of(AwsSqsSource.AWS_ACCESS_KEY_ID_PARAM, AwsSqsSource.AWS_SECRET_ACCESS_KEY_PARAM);
    }

    private String getBridgeWebhookUrl(String customerId, String bridgeId) {
        try {
            return new URL(gatewayConfiguratorService.getBridgeEndpoint(bridgeId, customerId)).toString();
        } catch (MalformedURLException e) {
            throw new GatewayProviderException("Can't find events webhook for bridge " + bridgeId);
        }
    }
}
