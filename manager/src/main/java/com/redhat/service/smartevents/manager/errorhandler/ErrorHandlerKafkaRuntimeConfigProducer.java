package com.redhat.service.smartevents.manager.errorhandler;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.smallrye.common.annotation.Identifier;

public class ErrorHandlerKafkaRuntimeConfigProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlerKafkaRuntimeConfigProducer.class);

    @Inject
    ResourceNamesProvider resourceNamesProvider;
    @Inject
    RhoasService rhoasService;

    @Produces
    @ApplicationScoped
    @UnlessBuildProfile("test")
    @Identifier("error-handler")
    public Map<String, Object> createKafkaRuntimeConfig() {
        String topic = resourceNamesProvider.getGlobalErrorTopicName();
        rhoasService.createTopicAndGrantAccessFor(topic, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
        LOGGER.info("Global error handler topic is \"{}\"", topic);
        return Map.of("topic", topic);
    }
}
