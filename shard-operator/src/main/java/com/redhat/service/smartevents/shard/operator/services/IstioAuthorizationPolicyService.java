package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.istio.authorizationpolicy.AuthorizationPolicy;

public interface IstioAuthorizationPolicyService {

    AuthorizationPolicy createIstioAuthorizationPolicy(BridgeIngress bridgeIngress);

    AuthorizationPolicy fetchIstioAuthorizationPolicy(BridgeIngress bridgeIngress);
}
