package com.redhat.service.smartevents.ingress.errorhandler;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class ErrorHandlerBridge {

    public static final String ERRORS_IN_CHANNEL = "errors-in";

    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlerBridge.class);

    @Incoming(ERRORS_IN_CHANNEL)
    public CompletionStage<Void> processEvent(final KafkaRecord<Integer, String> message) {
        try {
            String eventPayload = message.getPayload();
            LOG.info("===> ErrorEvent received: {}", eventPayload);
            message.getHeaders().forEach(h -> LOG.info("------> {} = {}", h.key(), new String(h.value(), StandardCharsets.UTF_8)));

        } catch (Exception e) {
            LOG.error("ErrorHandlerBridge for bridge '{}' failed to handle Event. The message is acked anyway.", 123, e);
        }
        return message.ack();
    }

}
