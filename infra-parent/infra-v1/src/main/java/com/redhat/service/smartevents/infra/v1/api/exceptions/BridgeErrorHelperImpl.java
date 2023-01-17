package com.redhat.service.smartevents.infra.v1.api.exceptions;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.exceptions.AbstractBridgeErrorHelper;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.v1.api.V1;

@V1
@ApplicationScoped
public class BridgeErrorHelperImpl extends AbstractBridgeErrorHelper {

    public BridgeErrorHelperImpl() {
        //CDI proxy
    }

    @Inject
    public BridgeErrorHelperImpl(@V1 BridgeErrorService bridgeErrorService) {
        super(bridgeErrorService);
    }

    @Override
    @PostConstruct
    protected void setup() {
        super.setup();
    }

}
