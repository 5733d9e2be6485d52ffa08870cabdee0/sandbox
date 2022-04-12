package com.redhat.service.rhose.infra.exceptions.definitions.platform;

public class TechnicalBearerTokenNotConfiguredException extends InternalPlatformException {
    public TechnicalBearerTokenNotConfiguredException(String message) {
        super(message);
    }

    public TechnicalBearerTokenNotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }
}
