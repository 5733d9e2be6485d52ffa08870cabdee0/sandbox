package com.redhat.developer.shard;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.ProcessorDTO;

public interface OperatorService {

    BridgeDTO createBridgeDeployment(BridgeDTO bridge);

    BridgeDTO deleteBridgeDeployment(BridgeDTO bridge);

    ProcessorDTO createProcessorDeployment(ProcessorDTO processor);
}
