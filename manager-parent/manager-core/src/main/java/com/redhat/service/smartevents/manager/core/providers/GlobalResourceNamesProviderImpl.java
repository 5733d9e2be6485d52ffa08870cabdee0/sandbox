package com.redhat.service.smartevents.manager.core.providers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class GlobalResourceNamesProviderImpl implements GlobalResourceNamesProvider {

    public static final String BRIDGE_SHORTNAME = "brdg";
    public static final String ERROR_TOPIC_SUFFIX = "err";

    private final ResourcePrefixProvider resourcePrefixProvider;

    @Inject
    public GlobalResourceNamesProviderImpl(ResourcePrefixProvider resourcePrefixProvider) {
        this.resourcePrefixProvider = resourcePrefixProvider;
    }

    @Override
    public String getGlobalErrorTopicName() {
        return String.format("%s%s", resourcePrefixProvider.getValidatedResourcePrefix(), ERROR_TOPIC_SUFFIX);
    }

    @Override
    public String getBridgeTopicName(String bridgeId) {
        return String.format("%s%s-%s", resourcePrefixProvider.getValidatedResourcePrefix(), BRIDGE_SHORTNAME, bridgeId);
    }

    @Override
    public String getBridgeErrorTopicName(String bridgeId) {
        return String.format("%s-%s", getBridgeTopicName(bridgeId), ERROR_TOPIC_SUFFIX);
    }
}
