package com.redhat.service.smartevents.infra.v1.api.exceptions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.exceptions.AbstractBridgeErrorServiceImpl;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorDAO;
import com.redhat.service.smartevents.infra.v1.api.V1;

@V1
@ApplicationScoped
public class BridgeErrorServiceImpl extends AbstractBridgeErrorServiceImpl {

    protected BridgeErrorServiceImpl() {
        // CDI proxy
    }

    @Inject
    public BridgeErrorServiceImpl(@V1 BridgeErrorDAO repository) {
        super(repository);
    }

}
