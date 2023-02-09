package com.redhat.service.smartevents.manager.core.providers;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ResourcePrefixProviderImpl implements ResourcePrefixProvider {
    public static final String RESOURCE_PREFIX_PROPERTY = "event-bridge.resource-prefix";

    private static final String VALIDATION_REGEX = "^[a-z][a-z0-9-]{0,16}$";

    @ConfigProperty(name = RESOURCE_PREFIX_PROPERTY)
    String resourcePrefix;

    private String validatedResourcePrefix;

    @PostConstruct
    void init() {
        validate();
    }

    public ResourcePrefixProviderImpl() {
    }

    public ResourcePrefixProviderImpl(String resourcePrefix) {
        this.resourcePrefix = resourcePrefix;
        validate();
    }

    private void validate() {
        if (resourcePrefix == null || resourcePrefix.isBlank() || !resourcePrefix.matches(VALIDATION_REGEX)) {
            throw new IllegalArgumentException(String.format("Property \"%s\" must match the regex \"%s\"", RESOURCE_PREFIX_PROPERTY, VALIDATION_REGEX));
        }
        validatedResourcePrefix = resourcePrefix + (resourcePrefix.endsWith("-") ? "" : "-");
    }

    public String getValidatedResourcePrefix() {
        return validatedResourcePrefix;
    }
}
