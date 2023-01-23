package com.redhat.service.smartevents.manager.v2.providers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.manager.core.providers.ResourcePrefixProvider;

@ApplicationScoped
public class ResourceNamesProviderV2Impl implements ResourceNamesProviderV2 {
    public static final String SOURCE_CONNECTOR_SHORTNAME = "mcsrc";

    private final ResourcePrefixProvider resourcePrefixProvider;

    @Inject
    public ResourceNamesProviderV2Impl(ResourcePrefixProvider resourcePrefixProvider) {
        this.resourcePrefixProvider = resourcePrefixProvider;
    }

    @Override
    public String getSourceConnectorName(String bridgeId) {
        return getSourceConnectorTopicName(bridgeId);
    }

    @Override
    public String getSourceConnectorTopicName(String bridgeId) {
        return String.format("%s%s-%s", resourcePrefixProvider.getValidatedResourcePrefix(), SOURCE_CONNECTOR_SHORTNAME, bridgeId);
    }
}
