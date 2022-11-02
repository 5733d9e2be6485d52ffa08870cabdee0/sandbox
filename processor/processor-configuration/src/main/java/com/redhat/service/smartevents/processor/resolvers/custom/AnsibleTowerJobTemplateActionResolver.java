package com.redhat.service.smartevents.processor.resolvers.custom;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.core.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class AnsibleTowerJobTemplateActionResolver implements AnsibleTowerJobTemplateAction, CustomGatewayResolver<Action> {

    public static final String LAUNCH_JOB_TEMPLATE_ENDPOINT_FORMAT = "%s/api/v2/job_templates/%s/launch/";

    @Override
    public Action resolve(Action action, String customerId, String bridgeId, String processorId) {
        Map<String, String> parameters = new HashMap<>();
        try {
            String launchTemplateEndpoint = getLaunchTemplateEndpoint(action);
            parameters.put(WebhookAction.ENDPOINT_PARAM, launchTemplateEndpoint);
        } catch (MalformedURLException e) {
            throw new GatewayProviderException("Can't build API call for ansible tower instance due to malformed URL", e);
        }

        if (action.hasParameter(BASIC_AUTH_USERNAME_PARAM)) {
            parameters.put(BASIC_AUTH_USERNAME_PARAM, action.getParameter(BASIC_AUTH_USERNAME_PARAM));
        }
        if (action.hasParameter(BASIC_AUTH_PASSWORD_PARAM)) {
            parameters.put(BASIC_AUTH_PASSWORD_PARAM, action.getParameter(BASIC_AUTH_PASSWORD_PARAM));
        }
        if (action.hasParameter(SSL_VERIFICATION_DISABLED)) {
            parameters.put(SSL_VERIFICATION_DISABLED, action.getParameter(SSL_VERIFICATION_DISABLED));
        }

        Action transformedAction = new Action();
        transformedAction.setType(WebhookAction.TYPE);
        transformedAction.setMapParameters(parameters);

        return transformedAction;
    }

    static String getLaunchTemplateEndpoint(Action action) throws MalformedURLException {
        String baseEndpoint = action.getParameter(ENDPOINT_PARAM);
        String jobTemplateId = action.getParameter(JOB_TEMPLATE_ID_PARAM);
        String fullEndpoint = String.format(LAUNCH_JOB_TEMPLATE_ENDPOINT_FORMAT, baseEndpoint, jobTemplateId);
        return new URL(fullEndpoint).toString();
    }
}
