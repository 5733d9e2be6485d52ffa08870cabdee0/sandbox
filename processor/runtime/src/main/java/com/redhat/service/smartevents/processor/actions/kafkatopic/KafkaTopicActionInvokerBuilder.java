package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
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
        String requiredTopic = action.getParameter(TOPIC_PARAM);

        return new KafkaTopicActionInvoker(emitter, processor, requiredTopic);
    }
}
