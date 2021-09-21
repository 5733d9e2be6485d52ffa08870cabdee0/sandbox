package com.redhat.service.bridge.executor;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

public interface ExecutorsK8SDeploymentManager {
    void deploy(ProcessorDTO processorDTO);

    void undeploy(String bridgeId, String processorId);
}
