package com.redhat.developer.manager;

import java.util.List;

import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.ListResult;

public interface BridgesService {

    Bridge createBridge(String customerId, BridgeRequest bridgeRequest);

    Bridge getBridge(String id, String customerId);

    ListResult<Bridge> getBridges(String customerId, int page, int pageSize);

    List<Bridge> getBridgesToDeploy();

    Bridge updateBridge(Bridge bridge);
}
