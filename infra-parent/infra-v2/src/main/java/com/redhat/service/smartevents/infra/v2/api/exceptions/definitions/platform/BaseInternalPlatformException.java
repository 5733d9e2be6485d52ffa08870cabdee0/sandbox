package com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformExceptionWithHref;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;

public abstract class BaseInternalPlatformException extends InternalPlatformExceptionWithHref {

    public BaseInternalPlatformException(String message) {
        super(message);
    }

    public BaseInternalPlatformException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getBaseHref() {
        return V2APIConstants.V2_ERROR_API_BASE_PATH;
    }
}
