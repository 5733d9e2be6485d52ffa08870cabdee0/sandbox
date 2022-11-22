package com.redhat.service.smartevents.shard.operator.core.providers;

import io.fabric8.kubernetes.api.model.Service;

public interface IstioGatewayProvider {

    Service getIstioGatewayService();

    Integer getIstioGatewayServicePort();
}
