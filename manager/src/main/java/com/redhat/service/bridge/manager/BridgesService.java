package com.redhat.service.bridge.manager;

import java.util.List;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.QueryInfo;

public interface BridgesService {

    Bridge createBridge(String customerId, BridgeRequest bridgeRequest);

    Bridge getBridge(String id);

    Bridge getBridge(String id, String customerId);

    void deleteBridge(String id, String customerId);

    ListResult<Bridge> getBridges(String customerId, QueryInfo queryInfo);

    List<Bridge> getBridgesByStatuses(List<BridgeStatus> statuses);

    Bridge updateBridge(BridgeDTO bridgeDTO);

    BridgeDTO toDTO(Bridge bridge);

    BridgeResponse toResponse(Bridge bridge);
}
