package com.redhat.service.bridge.manager;

import java.util.List;

import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;

public interface BridgesService {

    Bridge createBridge(String customerId, BridgeRequest bridgeRequest);

    Bridge getBridge(String id);

    Bridge getBridge(String id, String customerId);

    void deleteBridge(String id, String customerId);

    ListResult<Bridge> getBridges(String customerId, int page, int pageSize);

    List<Bridge> getBridgesByStatuses(List<BridgeStatus> statuses);

    Bridge updateBridge(Bridge bridge);
}
