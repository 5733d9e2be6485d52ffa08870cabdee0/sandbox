package com.redhat.service.smartevents.test.exceptions;

import java.util.Collection;
import java.util.HashSet;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public class ExceptionHelper {
    private static final String ROOT_USER_PACKAGE_NAME = "com.redhat.service.smartevents.infra.core.exceptions.definitions.user";
    private static final String ROOT_PLATFORM_PACKAGE_NAME = "com.redhat.service.smartevents.infra.core.exceptions.definitions.platform";
    private static final String INTERNAL_PLATFORM_EXCEPTION_NAME = "com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException";
    private static Collection<Class<?>> userExceptionClasses = new HashSet<>();
    private static Collection<Class<?>> platformExceptionClasses = new HashSet<>();

    static {
        loadUserExceptions();
        loadPlatformExceptions();
    }

    public static Collection<Class<?>> getUserExceptions() {
        return userExceptionClasses;
    }

    public static Collection<Class<?>> getPlatformExceptions() {
        return platformExceptionClasses;
    }

    private static void loadUserExceptions() {
        try (ScanResult scanResult =
                new ClassGraph()
                        .acceptPackages(ROOT_USER_PACKAGE_NAME)
                        .acceptClasses(INTERNAL_PLATFORM_EXCEPTION_NAME) // InternalPlatformException only should be visible by the user catalog
                        .scan()) {
            loadClasses(scanResult, RuntimeException.class.getName(), userExceptionClasses);
        }
    }

    private static void loadPlatformExceptions() {
        try (ScanResult scanResult =
                new ClassGraph()
                        .acceptPackages(ROOT_PLATFORM_PACKAGE_NAME)
                        .rejectClasses(INTERNAL_PLATFORM_EXCEPTION_NAME)
                        .scan()) {
            loadClasses(scanResult, RuntimeException.class.getName(), platformExceptionClasses);
        }
    }

    private static void loadClasses(ScanResult scanResult, String className, Collection<Class<?>> collection) {
        ClassInfoList classes = scanResult.getSubclasses(className);
        if (!classes.isEmpty()) {
            collection.addAll(classes.loadClasses());
            classes.forEach(c -> loadClasses(scanResult, c.getName(), collection));
        }
    }

    private ExceptionHelper() {
    }
}
