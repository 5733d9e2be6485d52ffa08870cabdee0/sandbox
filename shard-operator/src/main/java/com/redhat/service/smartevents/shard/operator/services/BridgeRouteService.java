package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;

public interface BridgeRouteService {

    Route createBridgeRoute(BridgeIngress bridgeIngress);

    Route fetchBridgeRoute(BridgeIngress bridgeIngress);
}
