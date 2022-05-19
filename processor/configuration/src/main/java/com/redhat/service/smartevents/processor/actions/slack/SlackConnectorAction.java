package com.redhat.service.smartevents.processor.actions.slack;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.processor.actions.generic.GenericConnectorAction;

@ApplicationScoped
public class SlackConnectorAction extends GenericConnectorAction {

    public static final String CONNECTOR_TYPE_ID = "slack_sink_0.1";

    public SlackConnectorAction() {
        super(CONNECTOR_TYPE_ID);
    }

    @Override
    public String getType() {
        return CONNECTOR_TYPE_ID;
    }
}
