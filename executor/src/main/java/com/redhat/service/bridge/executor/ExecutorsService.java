package com.redhat.service.bridge.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.BridgeCloudEventExtension;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.ExtensionProvider;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ExecutorsService {

    /**
     * Kafka Topic that we expect to have configured for receiving events.
     */
    public static final String EVENTS_IN_TOPIC = "events-in";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorsService.class);

    private Map<String, Set<Executor>> bridgeToProcessorMap = new HashMap<>();

    @Inject
    ExecutorFactory executorFactory;

    public void init(@Observes StartupEvent ev) {
        ExtensionProvider.getInstance().registerExtension(BridgeCloudEventExtension.class, BridgeCloudEventExtension::new);
    }

    public void createExecutor(ProcessorDTO processorDTO) {

        Executor executor = executorFactory.createExecutor(processorDTO);

        synchronized (bridgeToProcessorMap) {
            Set<Executor> executors = bridgeToProcessorMap.get(processorDTO.getBridge().getId());
            if (executors == null) {
                executors = new HashSet<>();
            }

            executors.add(executor);
            bridgeToProcessorMap.put(processorDTO.getBridge().getId(), executors);
        }
    }

    @Incoming(EVENTS_IN_TOPIC)
    public void processBridgeEvent(String event) {

        try {
            CloudEvent cloudEvent = CloudEventUtils.decode(event);
            BridgeCloudEventExtension bridgeCloudEventExtension = ExtensionProvider.getInstance().parseExtension(BridgeCloudEventExtension.class, cloudEvent);
            Set<Executor> executors = bridgeToProcessorMap.get(bridgeCloudEventExtension.getBridgeId());
            if (executors != null) {
                for (Executor e : executors) {
                    try {
                        e.onEvent(cloudEvent);
                    } catch (Throwable t) {
                        // Inner Throwable catch is to provide more specific context around which Executor failed to handle the Event, rather than a generic failure
                        LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event.", e.getProcessor().getId(), e.getProcessor().getBridge().getId(), t);
                    }
                }
            }
        } catch (Throwable t) {
            LOG.error("Failed to handle Event received on Bridge.", t);
        }
    }
}
