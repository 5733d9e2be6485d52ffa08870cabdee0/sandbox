package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.resources.Condition;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Set;

public interface ManagerClient {

    Uni<List<BridgeDTO>> fetchBridgesToDeployOrDelete();

    Uni<List<ProcessorDTO>> fetchProcessorsToDeployOrDelete();

    void notifyBridgeStatusChange(String bridgeId, Set<Condition> conditions);

    void notifyProcessorStatusChange(String processorId, Set<Condition> conditions);
}
