package com.redhat.service.bridge.shard;

import com.redhat.service.bridge.infra.dto.BridgeDTO;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;

public interface OperatorService {

    BridgeDTO createBridgeDeployment(BridgeDTO bridge);

    BridgeDTO deleteBridgeDeployment(BridgeDTO bridge);

    ProcessorDTO createProcessorDeployment(ProcessorDTO processor);
}
