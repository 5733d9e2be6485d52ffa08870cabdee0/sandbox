package com.redhat.service.smartevents.manager.v2.services;

import java.util.List;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;

public interface BridgeService {

    Bridge getBridge(String bridgeId, String customerId);

    Bridge getReadyBridge(String bridgeId, String customerId);

    Bridge createBridge(String customerId, String organisationId, String owner, BridgeRequest bridgeRequest);

    ListResult<Bridge> getBridges(String customerId, QueryResourceInfo queryInfo);

    List<Bridge> findByShardIdToDeployOrDelete(String shardId);

    BridgeDTO toDTO(Bridge processor);

    BridgeResponse toResponse(Bridge bridge);
}
