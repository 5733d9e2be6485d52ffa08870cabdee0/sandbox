package com.redhat.service.smartevents.infra.v2.api.exceptions;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.exceptions.AbstractBridgeErrorInMemoryDAO;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ItemNotFoundException;

@V2
@ApplicationScoped
public class BridgeErrorInMemoryDAO extends AbstractBridgeErrorInMemoryDAO {

    @Override
    @PostConstruct
    protected void init() {
        super.init();
    }

    @Override
    protected ExternalUserException getItemNotFoundException(String message) {
        return new ItemNotFoundException(message);
    }

}
