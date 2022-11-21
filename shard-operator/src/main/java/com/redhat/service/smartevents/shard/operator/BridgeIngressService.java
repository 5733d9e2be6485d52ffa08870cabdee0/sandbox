package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;

import java.util.List;

public interface BridgeIngressService {

    List<BridgeIngress> fetchAllBridgeIngress();

    BridgeIngress fetchBridgeIngress(String name, String namespace);
}
