package com.redhat.service.bridge.executor;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

public interface ExecutorK8SDeploymentManagerMock {
    void deploy(ProcessorDTO processorDTO);

    void undeploy(String bridgeId, String processorId);
}
