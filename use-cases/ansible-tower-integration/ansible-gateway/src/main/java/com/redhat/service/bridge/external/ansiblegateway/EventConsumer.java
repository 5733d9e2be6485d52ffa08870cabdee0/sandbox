package com.redhat.service.bridge.external.ansiblegateway;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class EventConsumer {

    public static final String EVENTS_IN_TOPIC = "events-in";

    private static final Logger LOG = LoggerFactory.getLogger(EventConsumer.class);

    @Inject
    ObjectMapper mapper;

    @Inject
    @RestClient
    AnsibleTowerClient ansibleTowerClient;

    @Incoming(EVENTS_IN_TOPIC)
    public CompletionStage<Void> processBridgeEvent(final Message<String> message) {
        try {
            JobTemplateEvent event = decode(message.getPayload());
            LOG.info("Received event with job template id {}", event.jobTemplateId);
            ansibleTowerClient.launchJobTemplate(event.jobTemplateId);
        } catch (RuntimeException e) {
            LOG.error("Failed to handle Event received on Ansible gateway. The message is acked anyway.", e);
        }

        return message.ack();
    }

    private JobTemplateEvent decode(String json) {
        try {
            return mapper.reader().readValue(json, JobTemplateEvent.class);
        } catch (IOException e) {
            LOG.error("Unable to decode CloudEvent", e);
            throw new RuntimeException(e);
        }
    }

}
