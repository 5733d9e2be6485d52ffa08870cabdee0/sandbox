package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

public interface BridgeIngressService {

    Ingress createBridgeIngress(BridgeIngress bridgeIngress, String path);

    Ingress fetchBridgeIngress(BridgeIngress bridgeIngress);
}
