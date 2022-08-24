package com.redhat.service.smartevents.manager;

import com.redhat.service.smartevents.infra.models.gateways.Action;

public interface ErrorHandlerService {

    Action getDefaultErrorHandlerAction();
}
