package com.redhat.service.smartevents.infra.exceptions.definitions.platform;

/**
 * This class represents all the exceptions that are not caused by the user mistake. i.e. OB fails to deploy something
 * due to internal issues.
 *
 * Only this class is included in the /resources/exceptionInfo.json. All the subclasses represent internal errors,
 * they are not visible by the user on the catalog.
 */
public class InternalPlatformException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InternalPlatformException(String message) {
        super(message);
    }

    public InternalPlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
