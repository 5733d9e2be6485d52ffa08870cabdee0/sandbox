package com.redhat.service.bridge.executor;

import com.redhat.service.bridge.infra.dto.ProcessorDTO;

public interface K8SDeploymentManagerMock {
    void deploy(ProcessorDTO processorDTO);

    void undeploy(String bridgeId, String processorId);

    void undeployAll();
}
