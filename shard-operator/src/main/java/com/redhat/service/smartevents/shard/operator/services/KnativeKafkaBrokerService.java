package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;

public interface KnativeKafkaBrokerService {

    KnativeBroker createKnativeKafkaBroker(BridgeIngress bridgeIngress);

    KnativeBroker fetchKnativeKafkaBroker(BridgeIngress bridgeIngress);

    String extractBrokerPath(BridgeIngress bridgeIngress);
}
