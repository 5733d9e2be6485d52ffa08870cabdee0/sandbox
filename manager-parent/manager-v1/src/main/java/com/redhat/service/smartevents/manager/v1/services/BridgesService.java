package com.redhat.service.smartevents.manager.v1.services;

import java.util.List;

import com.redhat.service.smartevents.infra.core.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.manager.v1.api.models.requests.BridgeRequestV1;
import com.redhat.service.smartevents.manager.v1.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;

public interface BridgesService {

    Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequestV1 bridgeRequestV1);

    Bridge updateBridge(String bridgeId, String customerId, BridgeRequestV1 bridgeRequestV1);

    Bridge getBridge(String id);

    Bridge getBridge(String id, String customerId);

    Bridge getReadyBridge(String bridgeId, String customerId);

    void deleteBridge(String id, String customerId);

    ListResult<Bridge> getBridges(String customerId, QueryResourceInfo queryInfo);

    List<Bridge> findByShardIdToDeployOrDelete(String shardId);

    Bridge updateBridgeStatus(ManagedResourceStatusUpdateDTO updateDTO);

    BridgeDTO toDTO(Bridge bridge);

    BridgeResponse toResponse(Bridge bridge);
}
