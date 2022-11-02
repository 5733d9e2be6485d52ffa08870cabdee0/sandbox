package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.ConfigMap;

public interface KnativeKafkaBrokerConfigMapService {

    ConfigMap createKnativeKafkaBrokerConfigMap(BridgeIngress bridgeIngress);

    ConfigMap fetchKnativeKafkaBrokerConfigMap(BridgeIngress bridgeIngress);
}
