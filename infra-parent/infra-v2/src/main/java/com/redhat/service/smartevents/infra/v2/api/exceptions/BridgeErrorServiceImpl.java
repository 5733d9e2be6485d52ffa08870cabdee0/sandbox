package com.redhat.service.smartevents.infra.v2.api.exceptions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.exceptions.AbstractBridgeErrorServiceImpl;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorDAO;
import com.redhat.service.smartevents.infra.v2.api.V2;

@V2
@ApplicationScoped
public class BridgeErrorServiceImpl extends AbstractBridgeErrorServiceImpl {

    protected BridgeErrorServiceImpl() {
        // CDI proxy
    }

    @Inject
    public BridgeErrorServiceImpl(@V2 BridgeErrorDAO repository) {
        super(repository);
    }

}
