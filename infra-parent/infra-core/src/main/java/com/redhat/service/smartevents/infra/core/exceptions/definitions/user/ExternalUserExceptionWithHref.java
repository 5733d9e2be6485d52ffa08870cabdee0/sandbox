package com.redhat.service.smartevents.infra.core.exceptions.definitions.user;

import com.redhat.service.smartevents.infra.core.exceptions.HasErrorHref;

public abstract class ExternalUserExceptionWithHref extends ExternalUserException implements HasErrorHref {

    private static final long serialVersionUID = 1L;

    public ExternalUserExceptionWithHref(String message) {
        super(message);
    }

    public ExternalUserExceptionWithHref(String message, Throwable cause) {
        super(message, cause);
    }

}
