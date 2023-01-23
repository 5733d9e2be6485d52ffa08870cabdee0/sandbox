package com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform;

public class OidcTokensNotInitializedException extends BaseInternalPlatformException {

    public OidcTokensNotInitializedException(String message) {
        super(message);
    }

    public OidcTokensNotInitializedException(String message, Throwable cause) {
        super(message, cause);
    }
}
