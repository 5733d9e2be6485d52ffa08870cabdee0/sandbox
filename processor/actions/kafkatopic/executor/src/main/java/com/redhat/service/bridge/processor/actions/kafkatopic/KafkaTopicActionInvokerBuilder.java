package com.redhat.service.bridge.processor.actions.kafkatopic;

import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.processor.actions.common.ActionInvoker;
import com.redhat.service.bridge.processor.actions.common.ActionInvokerBuilder;
import org.apache.kafka.clients.admin.AdminClient;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicAction.DEFAULT_LIST_TOPICS_TIMEOUT;
import static com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicAction.DEFAULT_LIST_TOPICS_TIMEUNIT;
import static com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicAction.TOPIC_PARAM;

@ApplicationScoped
public class KafkaTopicActionInvokerBuilder implements ActionInvokerBuilder {

    @Channel("actions-out")
    Emitter<String> emitter;

    @Inject
    AdminClient adminClient;

    @Override
    public String getType() {
        return KafkaTopicAction.TYPE;
    }

    @Override
    public ActionInvoker build(ProcessorDTO processor, BaseAction baseAction) {
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

        return new KafkaTopicActionInvoker(emitter, processor, requiredTopic);
    }
}
