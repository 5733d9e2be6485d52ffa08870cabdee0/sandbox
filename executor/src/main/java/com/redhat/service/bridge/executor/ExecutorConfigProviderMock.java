package com.redhat.service.bridge.executor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.dto.ProcessorDTO;

// TODO: This class has to be removed when we switch to ExecutorConfigProviderImpl
@ApplicationScoped
public class ExecutorConfigProviderMock implements ExecutorConfigProvider, K8SDeploymentManagerMock {

    private final Map<String, Set<Executor>> bridgeToProcessorMap = new HashMap<>();

    @Override
    public Set<Executor> getExecutors() {
        throw new UnsupportedOperationException("Not implemented by this mock.");
    }

    @Override
    public Set<Executor> getExecutors(String bridgeId) {
        return bridgeToProcessorMap.get(bridgeId);
    }

    @Override
    public void deploy(ProcessorDTO processorDTO) {

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

    @Override
    public void undeploy(String bridgeId, String processorId) {
        synchronized (bridgeToProcessorMap) {
            Set<Executor> executors = bridgeToProcessorMap.get(bridgeId);
            executors
                    .stream()
                    .filter(x -> x.getProcessor().getId().equals(processorId))
                    .findFirst()
                    .ifPresent(executors::remove);
        }
    }
}
