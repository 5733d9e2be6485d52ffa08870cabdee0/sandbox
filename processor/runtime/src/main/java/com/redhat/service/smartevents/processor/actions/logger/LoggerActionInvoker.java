package com.redhat.service.smartevents.processor.actions.logger;

import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicActionInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LoggerActionInvoker  implements ActionInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(LoggerActionInvoker.class);

    @Override
    public void onEvent(String event, Map<String, String> headers) {
        LOG.debug("Event : '{}'", event);
    }
}
