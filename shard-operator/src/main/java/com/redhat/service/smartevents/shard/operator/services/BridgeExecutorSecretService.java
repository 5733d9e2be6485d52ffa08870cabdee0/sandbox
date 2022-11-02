package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import io.fabric8.kubernetes.api.model.Secret;

public interface BridgeExecutorSecretService {

    Secret createBridgeExecutorSecret(BridgeExecutor bridgeExecutor);

    Secret fetchBridgeExecutorSecret(BridgeExecutor bridgeExecutor);
}
