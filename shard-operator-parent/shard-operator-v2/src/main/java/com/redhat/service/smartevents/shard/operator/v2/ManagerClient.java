package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;

import io.smallrye.mutiny.Uni;

public interface ManagerClient {

    Uni<List<BridgeDTO>> fetchBridgesToDeployOrDelete();
}
