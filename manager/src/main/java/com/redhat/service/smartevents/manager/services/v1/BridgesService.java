package com.redhat.service.smartevents.manager.services.v1;

import java.util.List;

import com.redhat.service.smartevents.infra.api.v1.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.api.v1.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.api.v1.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.v1.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.persistence.v1.models.Bridge;

public interface BridgesService {

    Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequest bridgeRequest);

    Bridge updateBridge(String bridgeId, String customerId, BridgeRequest bridgeRequest);

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
