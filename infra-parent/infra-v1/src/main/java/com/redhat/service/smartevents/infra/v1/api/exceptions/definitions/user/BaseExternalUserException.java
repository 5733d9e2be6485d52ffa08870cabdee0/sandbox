package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserExceptionWithHref;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;

public abstract class BaseExternalUserException extends ExternalUserExceptionWithHref {

    public BaseExternalUserException(String message) {
        super(message);
    }

    public BaseExternalUserException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getBaseHref() {
        return V1APIConstants.V1_ERROR_API_BASE_PATH;
    }
}
