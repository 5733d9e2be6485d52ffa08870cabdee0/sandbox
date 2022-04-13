package com.redhat.service.smartevents.processor.sources.slack;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.models.actions.Source;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.sources.SourceResolver;

@ApplicationScoped
public class SlackSourceResolver implements SourceResolver {

    @Override
    public String getType() {
        return SlackSource.TYPE;
    }

    @Override
    public Action resolve(Source source, String customerId, String bridgeId, String processorId) {
        Action resolvedAction = new Action();
        resolvedAction.setType(SendToBridgeAction.TYPE);
        resolvedAction.setParameters(Collections.emptyMap());
        return resolvedAction;
    }
}
