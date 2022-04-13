package com.redhat.service.smartevents.processor.sources;

import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.models.actions.Source;

public interface SourceResolver extends SourceBean {

    Action resolve(Source source, String customerId, String bridgeId, String processorId);
}
