package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;

import java.util.List;

public interface BridgeExecutorService {

    List<BridgeExecutor> fetchAllBridgeExecutor();
}
