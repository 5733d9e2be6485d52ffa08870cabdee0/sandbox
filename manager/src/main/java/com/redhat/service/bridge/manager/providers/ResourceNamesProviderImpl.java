package com.redhat.service.bridge.manager.providers;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ResourceNamesProviderImpl implements ResourceNamesProvider {

    public static final String KAFKA_TOPIC_PREFIX_PROPERTY = "event-bridge.resource-prefix";

    public static final String BRIDGE_SHORTNAME = "brdg";
    public static final String PROCESSOR_SHORTNAME = "prcs";

    @ConfigProperty(name = KAFKA_TOPIC_PREFIX_PROPERTY)
    String kafkaTopicPrefix;

    @Override
    public String getBridgeTopicName(String bridgeId) {
        return String.format("%s%s-%s", kafkaTopicPrefix, BRIDGE_SHORTNAME, bridgeId);
    }

    @Override
    public String getProcessorConnectorName(String processorId) {
        return getProcessorTopicName(processorId);
    }

    @Override
    public String getProcessorTopicName(String processorId) {
        return String.format("%s%s-%s", kafkaTopicPrefix, PROCESSOR_SHORTNAME, processorId);
    }
}
