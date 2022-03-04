package com.redhat.service.bridge.executor;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;

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
    public CompletionStage<Void> processBridgeEvent(final Message<String> message) {
        try {
            CloudEvent cloudEvent = CloudEventUtils.decode(message.getPayload());
            Executor executor = executorsProvider.getExecutor();
            try {
                executor.onEvent(cloudEvent);
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
