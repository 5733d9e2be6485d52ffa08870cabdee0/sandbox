package com.redhat.developer.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.infra.dto.ProcessorDTO;
import com.redhat.developer.infra.utils.CloudEventUtils;
import io.cloudevents.CloudEvent;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExecutorsService {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorsService.class);

    private Map<String, Set<Executor>> bridgeToProcessorMap = new HashMap<>();

    public void createExecutor(ProcessorDTO processorDTO) {

        if(BridgeStatus.AVAILABLE == processorDTO.getStatus()) {
            // throw an Exception, we've already created an Executor for this Processor
        }

        Executor executor = new Executor(processorDTO);

        synchronized (bridgeToProcessorMap) {
            Set<Executor> executors = bridgeToProcessorMap.get(processorDTO.getBridge().getId());
            if (executors == null) {
                executors = new HashSet<>();
            }

            executors.add(executor);
            bridgeToProcessorMap.put(processorDTO.getBridge().getId(), executors);
        }
    }

//    @Incoming("events-in")
    public void processBridgeEvent(String event) {
        CloudEvent cloudEvent = CloudEventUtils.decode(event);
        String bridgeId = cloudEvent.getAttribute("bridgeId").toString();
        Set<Executor> executors = bridgeToProcessorMap.get(bridgeId);
        if (executors != null) {
            for (Executor e : executors) {
                try {
                    e.onEvent(cloudEvent);
                } catch (Throwable t) {
                    LOG.error("Processor with name '{}' on bridge '{}' failed to handle Event.", e.getProcessor().getName(), e.getProcessor().getBridge().getId(), t);
                }
            }
        }
    }
}
