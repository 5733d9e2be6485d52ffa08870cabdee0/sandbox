package com.redhat.service.smartevents.manager.providers;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ResourceNamesProviderImpl implements ResourceNamesProvider {

    public static final String RESOURCE_PREFIX_PROPERTY = "event-bridge.resource-prefix";
    public static final String BRIDGE_SHORTNAME = "brdg";
    public static final String PROCESSOR_SHORTNAME = "prcs";

    private static final String VALIDATION_REGEX = "^[a-z][a-z0-9-]{0,19}$";

    @ConfigProperty(name = RESOURCE_PREFIX_PROPERTY)
    String resourcePrefix;

    String validatedResourcePrefix;

    @PostConstruct
    void validate() {
        if (resourcePrefix == null || resourcePrefix.isBlank() || !resourcePrefix.matches(VALIDATION_REGEX)) {
            throw new IllegalArgumentException(String.format("Property \"%s\" must match the regex \"%s\"", RESOURCE_PREFIX_PROPERTY, VALIDATION_REGEX));
        }
        validatedResourcePrefix = resourcePrefix + (resourcePrefix.endsWith("-") ? "" : "-");
    }

    @Override
    public String getBridgeTopicName(String bridgeId) {
        return String.format("%s%s-%s", validatedResourcePrefix, BRIDGE_SHORTNAME, bridgeId);
    }

    @Override
    public String getProcessorConnectorName(String processorId) {
        return getProcessorTopicName(processorId);
    }

    @Override
    public String getProcessorTopicName(String processorId) {
        return String.format("%s%s-%s", validatedResourcePrefix, PROCESSOR_SHORTNAME, processorId);
    }
}
