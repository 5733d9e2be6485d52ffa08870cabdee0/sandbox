package com.redhat.service.bridge.test.exceptions;

import java.util.Collection;
import java.util.HashSet;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public class ExceptionHelper {
    private static final String ROOT_USER_PACKAGE_NAME = "com.redhat.service.bridge.infra.exceptions.definitions.user";
    private static Collection<Class<?>> exceptionClasses;

    static {
        loadUserExceptions();
    }

    public static Collection<Class<?>> getUserExceptions() {
        return exceptionClasses;
    }

    private static void loadUserExceptions() {
        exceptionClasses = new HashSet<>();
        try (ScanResult scanResult =
                new ClassGraph()
                        .acceptPackages(ROOT_USER_PACKAGE_NAME)
                        .scan()) {
            loadClasses(scanResult, RuntimeException.class.getName());
        }
    }

    private static void loadClasses(ScanResult scanResult, String className) {
        ClassInfoList classes = scanResult.getSubclasses(className);
        if (!classes.isEmpty()) {
            exceptionClasses.addAll(classes.loadClasses());
            classes.forEach(c -> loadClasses(scanResult, c.getName()));
        }
    }

    private ExceptionHelper() {
    }
}
