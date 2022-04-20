package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionInvokerBuilder;

@ApplicationScoped
public class KafkaTopicActionInvokerBuilder implements KafkaTopicAction,
        ActionInvokerBuilder {

    public static final long DEFAULT_LIST_TOPICS_TIMEOUT = 10L;
    public static final TimeUnit DEFAULT_LIST_TOPICS_TIMEUNIT = TimeUnit.SECONDS;

    @Channel("actions-out")
    Emitter<String> emitter;

    @Inject
    AdminClient adminClient;

    @Override
    public ActionInvoker build(ProcessorDTO processor, Action action) {
        String requiredTopic = action.getParameters().get(TOPIC_PARAM);
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
        } catch (ExecutionException | TimeoutException e) {
            throw new ActionProviderException(String.format("Unable to list topics from Kafka to check requested topic '%s' exists for Action on Processor '%s' on bridge '%s'", requiredTopic,
                    processor.getId(), processor.getBridgeId()), e);
        } catch (InterruptedException e) {
            // Fixes SonarCloud bug: "InterruptedException" should not be ignored --
            // InterruptedExceptions should never be ignored in the code, and simply logging the exception counts in this case as "ignoring".
            // The throwing of the InterruptedException clears the interrupted state of the Thread, so if the exception is not handled properly
            // the information that the thread was interrupted will be lost.
            // Instead, InterruptedExceptions should either be rethrown - immediately or after cleaning up the method’s state - or the thread
            // should be re-interrupted by calling Thread.interrupt() even if this is supposed to be a single-threaded application.
            // Any other course of action risks delaying thread shutdown and loses the information that the thread was interrupted - probably
            // without finishing its task.
            Thread.currentThread().interrupt();
        }

        return new KafkaTopicActionInvoker(emitter, processor, requiredTopic);
    }
}
