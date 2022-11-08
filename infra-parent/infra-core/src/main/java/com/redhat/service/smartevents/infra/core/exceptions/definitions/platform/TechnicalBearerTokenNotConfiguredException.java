package com.redhat.service.smartevents.infra.core.exceptions.definitions.platform;

public class TechnicalBearerTokenNotConfiguredException extends InternalPlatformException {
    public TechnicalBearerTokenNotConfiguredException(String message) {
        super(message);
    }

    public TechnicalBearerTokenNotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }
}
