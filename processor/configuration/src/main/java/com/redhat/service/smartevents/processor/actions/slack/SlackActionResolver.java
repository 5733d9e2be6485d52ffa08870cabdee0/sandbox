package com.redhat.service.smartevents.processor.actions.slack;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.processor.actions.generic.GenericConnectorActionResolver;

@ApplicationScoped
public class SlackActionResolver extends GenericConnectorActionResolver {

    @Override
    public String getType() {
        return "slack_sink_0.1";
    }
}
