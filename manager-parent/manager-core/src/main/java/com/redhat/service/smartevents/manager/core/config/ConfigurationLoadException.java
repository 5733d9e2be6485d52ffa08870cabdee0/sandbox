package com.redhat.service.smartevents.manager.core.config;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;

public class ConfigurationLoadException extends InternalPlatformException {

    public ConfigurationLoadException(String message) {
        super(message);
    }

    public ConfigurationLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
