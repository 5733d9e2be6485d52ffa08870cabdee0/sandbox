package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform;

public class PrometheusNotInstalledException extends BaseInternalPlatformException {

    public PrometheusNotInstalledException(String message) {
        super(message);
    }

    public PrometheusNotInstalledException(String message, Throwable cause) {
        super(message, cause);
    }
}
