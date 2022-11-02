package com.redhat.service.smartevents.manager.core.processingerrors;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.manager.core.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.smallrye.common.annotation.Identifier;

public class ProcessingErrorKafkaRuntimeConfigProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingErrorKafkaRuntimeConfigProducer.class);

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    RhoasService rhoasService;

    @Produces
    @ApplicationScoped
    @UnlessBuildProfile("test")
    @Identifier("processing-errors")
    public Map<String, Object> createKafkaRuntimeConfig() {
        String topic = resourceNamesProvider.getGlobalErrorTopicName();
        rhoasService.createTopicAndGrantAccessFor(topic, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        LOGGER.info("Global error handler topic is \"{}\"", topic);
        return Map.of("topic", topic);
    }
}
