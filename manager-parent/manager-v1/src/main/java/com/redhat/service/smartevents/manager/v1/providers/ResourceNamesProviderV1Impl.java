package com.redhat.service.smartevents.manager.v1.providers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.manager.core.providers.ResourcePrefixProvider;

@ApplicationScoped
public class ResourceNamesProviderV1Impl implements ResourceNamesProviderV1 {
    public static final String PROCESSOR_SHORTNAME = "prcs";

    private final ResourcePrefixProvider resourcePrefixProvider;

    @Inject
    public ResourceNamesProviderV1Impl(ResourcePrefixProvider resourcePrefixProvider) {
        this.resourcePrefixProvider = resourcePrefixProvider;
    }

    @Override
    public String getProcessorConnectorName(String processorId) {
        return getProcessorTopicName(processorId);
    }

    @Override
    public String getProcessorTopicName(String processorId) {
        return String.format("%s%s-%s", resourcePrefixProvider.getValidatedResourcePrefix(), PROCESSOR_SHORTNAME, processorId);
    }
}
