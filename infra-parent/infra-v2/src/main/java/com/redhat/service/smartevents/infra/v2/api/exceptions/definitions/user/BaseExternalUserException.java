package com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserExceptionWithHref;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;

public abstract class BaseExternalUserException extends ExternalUserExceptionWithHref {

    public BaseExternalUserException(String message) {
        super(message);
    }

    public BaseExternalUserException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getBaseHref() {
        return V2APIConstants.V2_ERROR_API_BASE_PATH;
    }
}
