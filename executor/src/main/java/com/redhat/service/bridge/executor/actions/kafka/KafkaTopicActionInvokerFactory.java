package com.redhat.service.bridge.executor.actions.kafka;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.redhat.service.bridge.executor.actions.ActionInvoker;
import com.redhat.service.bridge.executor.actions.ActionInvokerException;
import com.redhat.service.bridge.executor.actions.ActionInvokerFactory;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.actions.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;

@ApplicationScoped
public class KafkaTopicActionInvokerFactory implements ActionInvokerFactory {

    static final long DEFAULT_LIST_TOPICS_TIMEOUT = 10L;

    static final TimeUnit DEFAULT_LIST_TOPICS_TIMEUNIT = TimeUnit.SECONDS;

    @Channel("actions-out")
    Emitter<CloudEvent> emitter;

    @Inject
    AdminClient adminClient;

    @Override
    public boolean accepts(BaseAction baseAction) {
        if (baseAction != null) {
            return KafkaTopicAction.KAFKA_ACTION_TYPE.equals(baseAction.getType());
        }
        return false;
    }

    @Override
    public ActionInvoker build(ProcessorDTO processor, BaseAction baseAction) {

        String requiredTopic = baseAction.getParameters().get(KafkaTopicAction.KAFKA_ACTION_TOPIC_PARAM);
        if (requiredTopic == null) {
            throw new ActionInvokerException(
                    String.format("There is no topic specified in the parameters for Action on Processor '%s' on Bridge '%s'", processor.getId(), processor.getBridge().getId()));
        }

        try {
            Set<String> strings = adminClient.listTopics().names().get(DEFAULT_LIST_TOPICS_TIMEOUT, DEFAULT_LIST_TOPICS_TIMEUNIT);
            if (!strings.contains(requiredTopic)) {
                throw new ActionInvokerException(
                        String.format("The requested topic '%s' for Action on Processor '%s' for bridge '%s' does not exist", requiredTopic, processor.getId(), processor.getBridge().getId()));
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new ActionInvokerException(String.format("Unable to list topics from Kafka to check requested topic '%s' exists for Action on Processor '%s' on bridge '%s'", requiredTopic,
                    processor.getId(), processor.getBridge().getId()), e);
        }

        return new KafkaTopicActionInvoker(emitter, processor, requiredTopic);
    }
}
