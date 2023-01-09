package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class TechnicalBearerTokenNotConfiguredException extends BaseInternalPlatformException {
    public TechnicalBearerTokenNotConfiguredException(String message) {
        super(message);
    }

    public TechnicalBearerTokenNotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }
}
