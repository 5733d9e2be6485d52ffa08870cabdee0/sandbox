package com.redhat.service.bridge.manager;

import java.util.List;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.models.Bridge;

public interface BridgesService {

    Bridge createBridge(String customerId, BridgeRequest bridgeRequest);

    Bridge getBridge(String id);

    Bridge getReadyBridge(String idOrName, String customerId);

    Bridge getBridgeByIdOrName(String idOrName, String customerId);

    void deleteBridge(String idOrName, String customerId);

    ListResult<Bridge> getBridges(String customerId, QueryInfo queryInfo);

    List<Bridge> getBridgesByStatusesAndShardId(List<BridgeStatus> statuses, String shardId);

    Bridge updateBridge(BridgeDTO bridgeDTO);

    BridgeDTO toDTO(Bridge bridge);

    BridgeResponse toResponse(Bridge bridge);

    String getBridgeTopicName(Bridge bridge);
}
