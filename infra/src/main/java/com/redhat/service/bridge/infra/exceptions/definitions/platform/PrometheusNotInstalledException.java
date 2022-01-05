package com.redhat.service.bridge.infra.exceptions.definitions.platform;

public class PrometheusNotInstalledException extends InternalPlatformException {

    public PrometheusNotInstalledException(String message) {
        super(message);
    }

    public PrometheusNotInstalledException(String message, Throwable cause) {
        super(message, cause);
    }
}
