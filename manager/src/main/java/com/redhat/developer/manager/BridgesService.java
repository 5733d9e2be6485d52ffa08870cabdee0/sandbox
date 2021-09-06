package com.redhat.developer.manager;

import java.util.List;

import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.models.Bridge;

public interface BridgesService {

    Bridge createBridge(String customerId, BridgeRequest bridgeRequest);

    List<Bridge> getBridges(String customerId);

    List<Bridge> getBridgesToDeploy();

    Bridge updateBridge(Bridge bridge);
}
