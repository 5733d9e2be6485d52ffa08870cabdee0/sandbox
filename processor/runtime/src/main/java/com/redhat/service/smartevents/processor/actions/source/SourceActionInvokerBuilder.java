package com.redhat.service.smartevents.processor.actions.source;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.auth.AbstractOidcClient;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.AbstractWebClientInvokerBuilder;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import static com.redhat.service.smartevents.processor.sources.slack.SlackSource.CLOUD_EVENT_TYPE;

@ApplicationScoped
public class SourceActionInvokerBuilder extends AbstractWebClientInvokerBuilder implements SourceAction {

    @Override
    public ActionInvoker build(ProcessorDTO processor, Action action) {
        String endpoint = action.getParameters().get(ENDPOINT_PARAM);
        String cloudEventType = action.getParameters().get(CLOUD_EVENT_TYPE);
        AbstractOidcClient abstractOidcClient = getOidcClient();
        return new SourceActionInvoker(endpoint, cloudEventType, webClient, abstractOidcClient);
    }
}
