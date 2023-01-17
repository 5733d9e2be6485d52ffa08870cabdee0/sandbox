package com.redhat.service.smartevents.infra.v1.api.auth;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.auth.AbstractIdentityResolverImpl;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user.ForbiddenRequestException;

@V1
@ApplicationScoped
public class IdentityResolverImpl extends AbstractIdentityResolverImpl {

    @Override
    protected ExternalUserException getForbiddenRequestException(String message) {
        return new ForbiddenRequestException(message);
    }

}
