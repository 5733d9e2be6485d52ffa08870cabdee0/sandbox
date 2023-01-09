package com.redhat.service.smartevents.infra.core.exceptions.definitions.platform;

import com.redhat.service.smartevents.infra.core.exceptions.HasErrorHref;

public abstract class InternalPlatformExceptionWithHref extends InternalPlatformException implements HasErrorHref {

    private static final long serialVersionUID = 1L;

    public InternalPlatformExceptionWithHref(String message) {
        super(message);
    }

    public InternalPlatformExceptionWithHref(String message, Throwable cause) {
        super(message, cause);
    }

}
