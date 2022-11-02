package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.Secret;

public interface KnativeKafkaBrokerSecretService {

    Secret createKnativeKafkaBrokerSecret(BridgeIngress bridgeIngress);

    Secret fetchKnativeKafkaBrokerSecret(BridgeIngress bridgeIngress);
}
