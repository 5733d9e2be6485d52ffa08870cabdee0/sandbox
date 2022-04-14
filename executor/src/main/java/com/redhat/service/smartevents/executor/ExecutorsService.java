package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

@ApplicationScoped
public class ExecutorsService {

    /**
     * Channel used for receiving events.
     */
    public static final String EVENTS_IN_CHANNEL = "events-in";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorsService.class);

    @Inject
    ExecutorsProvider executorsProvider;

    @Incoming(EVENTS_IN_CHANNEL)
    public CompletionStage<Void> processBridgeEvent(final IncomingKafkaRecord<Integer, String> message) {
        try {
            Map<String, String> headers = new HashMap<>();
            for (Header h : message.getHeaders().toArray()) {
                headers.put(h.key().substring(3), new String(h.value())); // TODO: refactor
            }

            CloudEvent ce = new CloudEventBuilder() // TODO: refactor
                    .withData(message.getPayload().getBytes(StandardCharsets.UTF_8))
                    .withId(headers.get("id"))
                    .withSource(new URI(headers.get("source")))
                    .withType(headers.get("type"))
                    .withDataSchema(new URI(headers.get("dataschema")))
                    .withSubject(headers.get("subject"))
                    .build();
            Executor executor = executorsProvider.getExecutor();
            try {
                executor.onEvent(ce);
            } catch (Throwable t) {
                // Inner Throwable catch is to provide more specific context around which Executor failed to handle the Event, rather than a generic failure
                LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event. The message is acked anyway.", executor.getProcessor().getId(),
                        executor.getProcessor().getBridgeId(), t);
            }
        } catch (Throwable t) {
            LOG.error("Failed to handle Event received on Bridge. The message is acked anyway.", t);
        }

        return message.ack();
    }
}
