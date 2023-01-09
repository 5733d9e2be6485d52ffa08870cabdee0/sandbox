package com.redhat.service.smartevents.infra.v2.api.auth;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.auth.AbstractIdentityResolverImpl;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ForbiddenRequestException;

@V2
@ApplicationScoped
public class IdentityResolverImpl extends AbstractIdentityResolverImpl {

    @Override
    protected ExternalUserException getForbiddenRequestException(String message) {
        return new ForbiddenRequestException(message);
    }

}
