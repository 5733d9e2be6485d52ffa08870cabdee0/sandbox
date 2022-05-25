package com.redhat.service.smartevents.processor.errorhandler;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
public class KafkaErrorPublisher implements ErrorPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaErrorPublisher.class);

    private final BroadcastProcessor<String> eventSubject = BroadcastProcessor.create();

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void sendError(ErrorMetadata errorMetadata, String payload, Exception exception) {
        sendError(errorMetadata, payload, exception.getMessage());
    }

    @Override
    public void sendError(ErrorMetadata metadata, String payload, String message) {
        LOGGER.info("Sending Error '{}' to error queue", message);

        Error error = new Error(metadata, payload, message);
        try {
            String item = objectMapper.writeValueAsString(error);
            eventSubject.onNext(item);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to send Error '{}' to error queue.", message, e);
        }
    }

    @Outgoing("errors-out")
    public Publisher<String> getEventPublisher() {
        return eventSubject.toHotStream();
    }
}
