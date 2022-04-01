package com.redhat.service.bridge.actions.kafkatopic;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

@ApplicationScoped
public class KafkaTopicAction implements ActionProvider {

    public static final String TYPE = "KafkaTopic";

    public static final String TOPIC_PARAM = "topic";

    public static final long DEFAULT_LIST_TOPICS_TIMEOUT = 10L;

    public static final TimeUnit DEFAULT_LIST_TOPICS_TIMEUNIT = TimeUnit.SECONDS;

    @Channel("actions-out")
    Emitter<String> emitter;

    @Inject
    AdminClient adminClient;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public ActionInvoker getActionInvoker(ProcessorDTO processor, BaseAction baseAction) {
        String requiredTopic = baseAction.getParameters().get(TOPIC_PARAM);
        if (requiredTopic == null) {
            throw new ActionProviderException(
                    String.format("There is no topic specified in the parameters for Action on Processor '%s' on Bridge '%s'", processor.getId(), processor.getBridgeId()));
        }

        try {
            Set<String> strings = adminClient.listTopics().names().get(DEFAULT_LIST_TOPICS_TIMEOUT, DEFAULT_LIST_TOPICS_TIMEUNIT);
            if (!strings.contains(requiredTopic)) {
                throw new ActionProviderException(
                        String.format("The requested topic '%s' for Action on Processor '%s' for bridge '%s' does not exist", requiredTopic, processor.getId(), processor.getBridgeId()));
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new ActionProviderException(String.format("Unable to list topics from Kafka to check requested topic '%s' exists for Action on Processor '%s' on bridge '%s'", requiredTopic,
                    processor.getId(), processor.getBridgeId()), e);
        }

        return new KafkaTopicInvoker(emitter, processor, requiredTopic);
    }
}
