package com.redhat.service.smartevents.test.exceptions;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public class ExceptionHelper {

    public static final String V1_ROOT_USER_PACKAGE_NAME = "com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user";
    public static final String V1_ROOT_PLATFORM_PACKAGE_NAME = "com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform";

    public static final String V2_ROOT_USER_PACKAGE_NAME = "com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user";
    public static final String V2_ROOT_PLATFORM_PACKAGE_NAME = "com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform";

    private static final String INTERNAL_PLATFORM_EXCEPTION_NAME = "com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException";

    public static Collection<Class<?>> getUserExceptions(String packageName) {
        return loadUserExceptions(packageName);
    }

    public static Collection<Class<?>> getPlatformExceptions(String packageName) {
        return loadPlatformExceptions(packageName);
    }

    private static Collection<Class<?>> loadUserExceptions(String packageName) {
        Collection<Class<?>> classes = new HashSet<>();
        try (ScanResult scanResult =
                new ClassGraph()
                        .acceptPackages(packageName)
                        .acceptClasses(INTERNAL_PLATFORM_EXCEPTION_NAME) // InternalPlatformException only should be visible by the user catalog
                        .scan()) {
            loadClasses(scanResult, RuntimeException.class.getName(), classes);
        }
        return classes;
    }

    private static Collection<Class<?>> loadPlatformExceptions(String packageName) {
        Collection<Class<?>> classes = new HashSet<>();
        try (ScanResult scanResult =
                new ClassGraph()
                        .acceptPackages(packageName)
                        .rejectClasses(INTERNAL_PLATFORM_EXCEPTION_NAME)
                        .scan()) {
            loadClasses(scanResult, RuntimeException.class.getName(), classes);
        }
        return classes;
    }

    private static void loadClasses(ScanResult scanResult, String className, Collection<Class<?>> collection) {
        ClassInfoList classes = scanResult.getSubclasses(className);
        if (!classes.isEmpty()) {
            collection.addAll(removeAbstractClasses(classes).loadClasses());
            classes.forEach(c -> loadClasses(scanResult, c.getName(), collection));
        }
    }

    private static ClassInfoList removeAbstractClasses(ClassInfoList classes) {
        return new ClassInfoList(classes.stream().filter(ci -> !ci.isAbstract()).collect(Collectors.toList()));
    }

    private ExceptionHelper() {
    }
}
